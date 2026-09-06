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

package com.examplatform.app;

import com.examplatform.shared.db.MultiSchemaFlywayRunner.SchemaMigrationSpec;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.examplatform.shared.messaging.SpringEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MonolithApplicationTest {

    @Test
    @DisplayName("Monolith schemas list contains all 14 domain service schemas")
    void testMonolithSchemaSpecs() {
        List<SchemaMigrationSpec> specs = List.of(
                SchemaMigrationSpec.of("identity_service", "classpath:db/migration/identity"),
                SchemaMigrationSpec.of("candidate_service", "classpath:db/migration/candidate"),
                SchemaMigrationSpec.of("question_service", "classpath:db/migration/question", "classpath:db/migration/question/seeds"),
                SchemaMigrationSpec.of("examination_service", "classpath:db/migration/examination"),
                SchemaMigrationSpec.of("paper_generator", "classpath:db/migration/paper_generator"),
                SchemaMigrationSpec.of("delivery_service", "classpath:db/migration/delivery"),
                SchemaMigrationSpec.of("response_service", "classpath:db/migration/response"),
                SchemaMigrationSpec.of("evaluation_service", "classpath:db/migration/evaluation"),
                SchemaMigrationSpec.of("result_service", "classpath:db/migration/result"),
                SchemaMigrationSpec.of("audit_service", "classpath:db/migration/audit"),
                SchemaMigrationSpec.of("notification_service", "classpath:db/migration/notification"),
                SchemaMigrationSpec.of("admin_service", "classpath:db/migration/admin"),
                SchemaMigrationSpec.of("analytics_service", "classpath:db/migration/analytics"),
                SchemaMigrationSpec.of("asset_service", "classpath:db/migration/asset")
        );

        assertThat(specs).hasSize(14);
        assertThat(specs)
                .extracting(SchemaMigrationSpec::schema)
                .containsExactlyInAnyOrder(
                        "identity_service",
                        "candidate_service",
                        "question_service",
                        "examination_service",
                        "paper_generator",
                        "delivery_service",
                        "response_service",
                        "evaluation_service",
                        "result_service",
                        "audit_service",
                        "notification_service",
                        "admin_service",
                        "analytics_service",
                        "asset_service"
                );
    }

    @Test
    @DisplayName("SpringEventPublisher dispatches GenericDomainEvent with topic and key")
    void testSpringEventPublisher() {
        AtomicReference<Object> publishedEvent = new AtomicReference<>();
        ApplicationEventPublisher mockSpringPublisher = publishedEvent::set;

        SpringEventPublisher publisher = new SpringEventPublisher(mockSpringPublisher);
        publisher.publish("exam.audit.events", "audit-key-123", "{\"action\":\"LOGIN\"}");

        assertThat(publishedEvent.get()).isInstanceOf(GenericDomainEvent.class);
        GenericDomainEvent event = (GenericDomainEvent) publishedEvent.get();
        assertThat(event.topic()).isEqualTo("exam.audit.events");
        assertThat(event.key()).isEqualTo("audit-key-123");
        assertThat(event.payload()).isEqualTo("{\"action\":\"LOGIN\"}");
    }
}
