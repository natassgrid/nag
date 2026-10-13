/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.candidate.consumer;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import com.examplatform.shared.constants.MessagingConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.crypto.HashingService;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.examplatform.shared.messaging.MessagePayloadExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.examplatform.shared.constants.MessagingConstants.AUDIT_EVENTS_TOPIC;

/**
 * Listens for candidate registration audit/domain events across Kafka, RabbitMQ,
 * or Spring in-memory events to auto-provision a candidate profile upon registration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateRegistrationConsumer {
    private static final String HMAC_KEY_PREFIX = "candidate-doc-hmac-";
    private static final String DEK_PREFIX = "candidate-dek-";
    private static final String FIELD_FULL_NAME = "fullName";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_IDENTITY_DOC_NUMBER = "identityDocNumber";
    private static final String PENDING_PREFIX = "PENDING-";

    private final CandidateProfileRepository candidateProfileRepository;
    private final HashingService hashingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = AUDIT_EVENTS_TOPIC, groupId = "candidate-registration-provisioning")
    public void onKafkaAuditEvent(String message) {
        processRegistrationEvent(message);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "candidate.registration.provisioning.queue", durable = "true"),
                    exchange = @Exchange(value = MessagingConstants.EXAM_EVENTS_TOPIC, type = ExchangeTypes.TOPIC),
                    key = AUDIT_EVENTS_TOPIC
            )
    )
    public void onRabbitAuditEvent(Object message) {
        try {
            processRegistrationEvent(MessagePayloadExtractor.extractPayload(message, objectMapper));
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ registration audit event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void onSpringAuditEvent(GenericDomainEvent event) {
        if (!AUDIT_EVENTS_TOPIC.equals(event.topic())) {
            return;
        }
        try {
            processRegistrationEvent(MessagePayloadExtractor.extractPayload(event.payload(), objectMapper));
        } catch (Exception e) {
            log.error("Failed to process Spring registration audit event: {}", e.getMessage(), e);
        }
    }

    private void processRegistrationEvent(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!isRegistrationEvent(root)) {
                return;
            }

            String actorId = root.hasNonNull("actorId") ? root.path("actorId").asText("") : root.path("userId").asText("");
            if (actorId.isBlank()) {
                return;
            }

            Optional<UUID> userIdOpt = parseUserId(actorId);
            if (userIdOpt.isEmpty()) {
                return;
            }

            JsonNode details = root.path("details");
            RegistrationData data = new RegistrationData(
                    userIdOpt.get(),
                    extractField(root, details, SharedConstants.TENANT_ID, SharedConstants.DEFAULT_TENANT),
                    extractField(root, details, FIELD_FULL_NAME, ""),
                    extractField(root, details, FIELD_EMAIL, ""),
                    extractField(root, details, FIELD_MOBILE, ""),
                    extractField(root, details, FIELD_IDENTITY_DOC_NUMBER, "")
            );

            saveCandidateProfile(data);
        } catch (Exception e) {
            log.warn("Could not auto-provision candidate profile from registration event: {}", e.getMessage());
        }
    }

    private boolean isRegistrationEvent(JsonNode root) {
        String eventType = root.path(SharedConstants.EVENT_TYPE).asText("");
        String action = root.path(SharedConstants.ACTION).asText("");
        return "CANDIDATE_PROFILE_CREATED".equals(eventType) || "identity:registration".equals(action);
    }

    private Optional<UUID> parseUserId(String actorId) {
        try {
            return Optional.of(UUID.fromString(actorId));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    private String extractField(JsonNode root, JsonNode details, String fieldName, String defaultValue) {
        if (root.hasNonNull(fieldName)) {
            return root.path(fieldName).asText(defaultValue);
        }
        return details.path(fieldName).asText(defaultValue);
    }

    private void saveCandidateProfile(RegistrationData data) {
        Optional<CandidateProfile> existingOpt = candidateProfileRepository.findByUserIdAndTenantId(data.userId(), data.tenantId());
        String dekKeyName = DEK_PREFIX + data.userId();
        String mobileHash = !data.mobile().isBlank() ? hashingService.sha256(data.mobile().trim()) : PENDING_PREFIX + data.userId();
        String docHash = !data.docNumber().isBlank() ? hashingService.sha256(data.docNumber().trim().toUpperCase()) : PENDING_PREFIX + data.userId();
        String docHmac = !data.docNumber().isBlank() ? hashingService.hmac(data.docNumber().trim().toUpperCase(), HMAC_KEY_PREFIX + data.tenantId()) : PENDING_PREFIX + data.userId();

        CandidateProfile profile;
        if (existingOpt.isPresent()) {
            profile = existingOpt.get();
            updateExistingProfile(profile, data, mobileHash, docHash, docHmac, dekKeyName);
        } else {
            profile = createNewProfile(data, mobileHash, docHash, docHmac, dekKeyName);
        }

        candidateProfileRepository.save(profile);
        log.info("Successfully provisioned / updated CandidateProfile from registration event for userId={} in tenant={}", data.userId(), data.tenantId());
    }

    private void updateExistingProfile(
            CandidateProfile profile,
            RegistrationData data,
            String mobileHash,
            String docHash,
            String docHmac,
            String dekKeyName) {
        if (!data.fullName().isBlank()) {
            profile.setFullName(data.fullName());
        }
        if (!data.email().isBlank()) {
            profile.setEmail(data.email());
        }
        if (!data.mobile().isBlank()) {
            profile.setMobile(data.mobile());
            profile.setMobileHash(mobileHash);
        }
        if (!data.docNumber().isBlank()) {
            profile.setIdentityDocNumber(data.docNumber());
            profile.setIdentityDocHash(docHash);
            profile.setIdentityDocHmac(docHmac);
        }
        if (profile.getEncryptionKeyId() == null) {
            profile.setEncryptionKeyId(dekKeyName);
        }
    }

    private CandidateProfile createNewProfile(
            RegistrationData data,
            String mobileHash,
            String docHash,
            String docHmac,
            String dekKeyName) {
        CandidateProfile profile = CandidateProfile.builder()
                .userId(data.userId())
                .fullName(!data.fullName().isBlank() ? data.fullName() : null)
                .email(!data.email().isBlank() ? data.email() : null)
                .mobile(!data.mobile().isBlank() ? data.mobile() : null)
                .identityDocNumber(!data.docNumber().isBlank() ? data.docNumber() : null)
                .encryptionKeyId(dekKeyName)
                .mobileHash(mobileHash)
                .identityDocHash(docHash)
                .identityDocHmac(docHmac)
                .consentRecorded(false)
                .build();
        profile.setTenantId(data.tenantId());
        return profile;
    }

    private record RegistrationData(
            UUID userId,
            String tenantId,
            String fullName,
            String email,
            String mobile,
            String docNumber
    ) {}
}
