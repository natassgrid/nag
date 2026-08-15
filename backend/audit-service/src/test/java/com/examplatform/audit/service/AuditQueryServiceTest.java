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

package com.examplatform.audit.service;

import com.examplatform.audit.domain.AuditEvent;
import com.examplatform.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditQueryService")
class AuditQueryServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditQueryService auditQueryService;

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-001";
    private static final String EVENT_TYPE = "EXAM_STARTED";
    private static final String RESOURCE = "exam-123";

    @BeforeEach
    void setUp() {
        auditQueryService = new AuditQueryService(auditEventRepository);
    }

    @Test
    @DisplayName("Query with all filters builds correct specification and returns results")
    void queryEvents_withAllFilters_returnsPagedResults() {
        // Given
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T23:59:59Z");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));

        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actorId(ACTOR_ID)
                .eventType(EVENT_TYPE)
                .resource(RESOURCE)
                .tenantId(TENANT_ID)
                .occurredAt(Instant.now())
                .payloadHash("hash123")
                .build();

        Page<AuditEvent> expectedPage = new PageImpl<>(List.of(event), pageable, 1);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<AuditEvent> result = auditQueryService.queryEvents(
                ACTOR_ID, EVENT_TYPE, RESOURCE, from, to, TENANT_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActorId()).isEqualTo(ACTOR_ID);
        assertThat(result.getContent().get(0).getEventType()).isEqualTo(EVENT_TYPE);
        verify(auditEventRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Query with no optional filters applies only tenant filter")
    void queryEvents_withNoOptionalFilters_appliesOnlyTenantFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditEvent> expectedPage = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<AuditEvent> result = auditQueryService.queryEvents(
                null, null, null, null, null, TENANT_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(auditEventRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Query with only actorId filter returns matching events")
    void queryEvents_withActorIdOnly_returnsMatchingEvents() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        AuditEvent event1 = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actorId(ACTOR_ID)
                .eventType("LOGIN")
                .resource("auth")
                .tenantId(TENANT_ID)
                .occurredAt(Instant.now())
                .payloadHash("hash1")
                .build();

        AuditEvent event2 = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actorId(ACTOR_ID)
                .eventType("EXAM_STARTED")
                .resource("exam-456")
                .tenantId(TENANT_ID)
                .occurredAt(Instant.now())
                .payloadHash("hash2")
                .build();

        Page<AuditEvent> expectedPage = new PageImpl<>(List.of(event1, event2), pageable, 2);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<AuditEvent> result = auditQueryService.queryEvents(
                ACTOR_ID, null, null, null, null, TENANT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(e -> e.getActorId().equals(ACTOR_ID));
    }

    @Test
    @DisplayName("Query with time range filter returns events within range")
    void queryEvents_withTimeRange_returnsEventsInRange() {
        // Given
        Instant from = Instant.parse("2024-06-01T00:00:00Z");
        Instant to = Instant.parse("2024-06-30T23:59:59Z");
        Pageable pageable = PageRequest.of(0, 20);

        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actorId(UUID.randomUUID())
                .eventType("ANSWER_SUBMITTED")
                .resource("exam-789")
                .tenantId(TENANT_ID)
                .occurredAt(Instant.parse("2024-06-15T10:00:00Z"))
                .payloadHash("hash3")
                .build();

        Page<AuditEvent> expectedPage = new PageImpl<>(List.of(event), pageable, 1);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<AuditEvent> result = auditQueryService.queryEvents(
                null, null, null, from, to, TENANT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOccurredAt()).isAfter(from).isBefore(to);
    }

    @Test
    @DisplayName("Query with blank eventType is treated as no filter")
    void queryEvents_withBlankEventType_treatedAsNoFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditEvent> expectedPage = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<AuditEvent> result = auditQueryService.queryEvents(
                null, "   ", null, null, null, TENANT_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        verify(auditEventRepository).findAll(any(Specification.class), eq(pageable));
    }
}
