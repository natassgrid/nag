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

package com.examplatform.shared.audit;

import com.examplatform.shared.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JpaAuditingConfig Unit Tests")
class JpaAuditingConfigTest {

    private final JpaAuditingConfig config = new JpaAuditingConfig();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("Returns empty optional when UserContext is unauthenticated")
    void emptyWhenUnauthenticated() {
        AuditorAware<UUID> auditorAware = config.auditorAware();
        Optional<UUID> currentAuditor = auditorAware.getCurrentAuditor();

        assertThat(currentAuditor).isEmpty();
    }

    @Test
    @DisplayName("Returns current user UUID when UserContext is populated")
    void returnsCurrentUserUuid() {
        UUID userId = UUID.randomUUID();
        UserContext.set(userId, "author@exam.gov.in", "default", Set.of("ROLE_QUESTION_AUTHOR"));

        AuditorAware<UUID> auditorAware = config.auditorAware();
        Optional<UUID> currentAuditor = auditorAware.getCurrentAuditor();

        assertThat(currentAuditor).isPresent().contains(userId);
    }
}
