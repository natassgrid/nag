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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateRegistrationConsumer Unit Tests")
class CandidateRegistrationConsumerTest {

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private HashingService hashingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CandidateRegistrationConsumer consumer;

    private static final UUID USER_ID = UUID.fromString("01a0e16f-0393-7d38-ab74-48a47764b08e");
    private static final String TENANT_ID = "default";

    @BeforeEach
    void setUp() {
        consumer = new CandidateRegistrationConsumer(candidateProfileRepository, hashingService, objectMapper);
    }

    @Test
    @DisplayName("Provisions new CandidateProfile from root-level event fields")
    void provisionsNewCandidateProfileFromRootFields() {
        when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());
        when(hashingService.sha256("9876543210")).thenReturn("mock-mobile-hash");
        when(hashingService.sha256("ABCDE1234F")).thenReturn("mock-doc-hash");
        when(hashingService.hmac(eq("ABCDE1234F"), anyString())).thenReturn("mock-doc-hmac");

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "CANDIDATE_PROFILE_CREATED");
        payload.put("actorId", USER_ID.toString());
        payload.put("tenantId", TENANT_ID);
        payload.put("fullName", "Sheel Prabhakar");
        payload.put("email", "sheel.prabhakar@gmail.com");
        payload.put("mobile", "9876543210");
        payload.put("identityDocNumber", "ABCDE1234F");

        GenericDomainEvent event = new GenericDomainEvent("exam.audit.events", USER_ID.toString(), payload);
        consumer.onSpringAuditEvent(event);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(captor.capture());

        CandidateProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getFullName()).isEqualTo("Sheel Prabhakar");
        assertThat(saved.getEmail()).isEqualTo("sheel.prabhakar@gmail.com");
        assertThat(saved.getMobile()).isEqualTo("9876543210");
        assertThat(saved.getIdentityDocNumber()).isEqualTo("ABCDE1234F");
        assertThat(saved.getMobileHash()).isEqualTo("mock-mobile-hash");
        assertThat(saved.getIdentityDocHash()).isEqualTo("mock-doc-hash");
        assertThat(saved.getIdentityDocHmac()).isEqualTo("mock-doc-hmac");
        assertThat(saved.getEncryptionKeyId()).isEqualTo("candidate-dek-" + USER_ID);
    }

    @Test
    @DisplayName("Provisions new CandidateProfile from nested details event fields")
    void provisionsNewCandidateProfileFromDetailsFields() {
        when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());
        when(hashingService.sha256("9876543210")).thenReturn("mock-mobile-hash");
        when(hashingService.sha256("ABCDE1234F")).thenReturn("mock-doc-hash");
        when(hashingService.hmac(eq("ABCDE1234F"), anyString())).thenReturn("mock-doc-hmac");

        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", TENANT_ID);
        details.put("fullName", "Sheel Prabhakar");
        details.put("email", "sheel.prabhakar@gmail.com");
        details.put("mobile", "9876543210");
        details.put("identityDocNumber", "ABCDE1234F");

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "CANDIDATE_PROFILE_CREATED");
        payload.put("actorId", USER_ID.toString());
        payload.put("details", details);

        GenericDomainEvent event = new GenericDomainEvent("exam.audit.events", USER_ID.toString(), payload);
        consumer.onSpringAuditEvent(event);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(captor.capture());

        CandidateProfile saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Sheel Prabhakar");
        assertThat(saved.getEmail()).isEqualTo("sheel.prabhakar@gmail.com");
        assertThat(saved.getMobile()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("Updates existing profile if fields were previously missing")
    void updatesExistingProfileWhenFieldsMissing() {
        CandidateProfile existing = CandidateProfile.builder()
                .userId(USER_ID)
                .fullName(null)
                .email(null)
                .mobile(null)
                .identityDocNumber(null)
                .build();
        existing.setTenantId(TENANT_ID);

        when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
        when(hashingService.sha256("9876543210")).thenReturn("mock-mobile-hash");
        when(hashingService.sha256("ABCDE1234F")).thenReturn("mock-doc-hash");
        when(hashingService.hmac(eq("ABCDE1234F"), anyString())).thenReturn("mock-doc-hmac");

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "identity:registration");
        payload.put("actorId", USER_ID.toString());
        payload.put("tenantId", TENANT_ID);
        payload.put("fullName", "Sheel Prabhakar");
        payload.put("email", "sheel.prabhakar@gmail.com");
        payload.put("mobile", "9876543210");
        payload.put("identityDocNumber", "ABCDE1234F");

        consumer.onKafkaAuditEvent(objectMapper.valueToTree(payload).toString());

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(captor.capture());

        CandidateProfile saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Sheel Prabhakar");
        assertThat(saved.getEmail()).isEqualTo("sheel.prabhakar@gmail.com");
        assertThat(saved.getMobile()).isEqualTo("9876543210");
        assertThat(saved.getIdentityDocNumber()).isEqualTo("ABCDE1234F");
        assertThat(saved.getMobileHash()).isEqualTo("mock-mobile-hash");
    }

    @Test
    @DisplayName("Ignores non-registration audit events")
    void ignoresNonRegistrationEvents() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "USER_LOGIN_SUCCESS");
        payload.put("actorId", USER_ID.toString());

        GenericDomainEvent event = new GenericDomainEvent("exam.audit.events", USER_ID.toString(), payload);
        consumer.onSpringAuditEvent(event);

        verify(candidateProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ignores events from other non-audit topics")
    void ignoresOtherTopics() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "CANDIDATE_PROFILE_CREATED");
        payload.put("actorId", USER_ID.toString());

        GenericDomainEvent event = new GenericDomainEvent("other.topic", USER_ID.toString(), payload);
        consumer.onSpringAuditEvent(event);

        verify(candidateProfileRepository, never()).save(any());
    }
}
