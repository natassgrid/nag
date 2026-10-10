/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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
import com.examplatform.shared.crypto.HashingService;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for candidate registration audit/domain events across Kafka, RabbitMQ,
 * or Spring in-memory events to auto-provision a candidate profile upon registration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateRegistrationConsumer {

    public static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String HMAC_KEY_PREFIX = "candidate-doc-hmac-";
    private static final String DEK_PREFIX = "candidate-dek-";

    private final CandidateProfileRepository candidateProfileRepository;
    private final HashingService hashingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = AUDIT_TOPIC, groupId = "candidate-registration-provisioning")
    public void onKafkaAuditEvent(String message) {
        processRegistrationEvent(message);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "candidate.registration.provisioning.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = AUDIT_TOPIC
            )
    )
    public void onRabbitAuditEvent(Object message) {
        try {
            String payload;
            if (message instanceof Message amqpMsg) {
                payload = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
            } else if (message instanceof byte[] bytes) {
                payload = new String(bytes, StandardCharsets.UTF_8);
            } else if (message instanceof String s) {
                payload = s;
            } else {
                payload = objectMapper.writeValueAsString(message);
            }
            processRegistrationEvent(payload);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ registration audit event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void onSpringAuditEvent(GenericDomainEvent event) {
        if (!AUDIT_TOPIC.equals(event.topic())) {
            return;
        }
        try {
            Object payload = event.payload();
            String message;
            if (payload instanceof Message amqpMsg) {
                message = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
            } else if (payload instanceof byte[] bytes) {
                message = new String(bytes, StandardCharsets.UTF_8);
            } else if (payload instanceof String s) {
                message = s;
            } else {
                message = objectMapper.writeValueAsString(payload);
            }
            processRegistrationEvent(message);
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
            String eventType = root.path("eventType").asText("");
            String action = root.path("action").asText("");
            if (!"CANDIDATE_PROFILE_CREATED".equals(eventType) && !"identity:registration".equals(action)) {
                return;
            }

            String actorId = root.hasNonNull("actorId") ? root.path("actorId").asText("") : root.path("userId").asText("");
            if (actorId.isBlank()) {
                return;
            }

            UUID userId;
            try {
                userId = UUID.fromString(actorId);
            } catch (IllegalArgumentException e) {
                return;
            }

            JsonNode details = root.path("details");
            String tenantId = root.hasNonNull("tenantId") ? root.path("tenantId").asText("default") : details.path("tenantId").asText("default");
            String fullName = root.hasNonNull("fullName") ? root.path("fullName").asText("") : details.path("fullName").asText("");
            String email = root.hasNonNull("email") ? root.path("email").asText("") : details.path("email").asText("");
            String mobile = root.hasNonNull("mobile") ? root.path("mobile").asText("") : details.path("mobile").asText("");
            String docNumber = root.hasNonNull("identityDocNumber") ? root.path("identityDocNumber").asText("") : details.path("identityDocNumber").asText("");

            Optional<CandidateProfile> existingOpt = candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId);
            String dekKeyName = DEK_PREFIX + userId;
            String mobileHash = !mobile.isBlank() ? hashingService.sha256(mobile.trim()) : "PENDING-" + userId;
            String docHash = !docNumber.isBlank() ? hashingService.sha256(docNumber.trim().toUpperCase()) : "PENDING-" + userId;
            String docHmac = !docNumber.isBlank() ? hashingService.hmac(docNumber.trim().toUpperCase(), HMAC_KEY_PREFIX + tenantId) : "PENDING-" + userId;

            CandidateProfile profile;
            if (existingOpt.isPresent()) {
                profile = existingOpt.get();
                if (!fullName.isBlank()) profile.setFullName(fullName);
                if (!email.isBlank()) profile.setEmail(email);
                if (!mobile.isBlank()) {
                    profile.setMobile(mobile);
                    profile.setMobileHash(mobileHash);
                }
                if (!docNumber.isBlank()) {
                    profile.setIdentityDocNumber(docNumber);
                    profile.setIdentityDocHash(docHash);
                    profile.setIdentityDocHmac(docHmac);
                }
                if (profile.getEncryptionKeyId() == null) {
                    profile.setEncryptionKeyId(dekKeyName);
                }
            } else {
                profile = CandidateProfile.builder()
                        .userId(userId)
                        .fullName(!fullName.isBlank() ? fullName : null)
                        .email(!email.isBlank() ? email : null)
                        .mobile(!mobile.isBlank() ? mobile : null)
                        .identityDocNumber(!docNumber.isBlank() ? docNumber : null)
                        .encryptionKeyId(dekKeyName)
                        .mobileHash(mobileHash)
                        .identityDocHash(docHash)
                        .identityDocHmac(docHmac)
                        .consentRecorded(false)
                        .build();
                profile.setTenantId(tenantId);
            }

            candidateProfileRepository.save(profile);
            log.info("Successfully provisioned / updated CandidateProfile from registration event for userId={} in tenant={}", userId, tenantId);
        } catch (Exception e) {
            log.warn("Could not auto-provision candidate profile from registration event: {}", e.getMessage());
        }
    }
}
