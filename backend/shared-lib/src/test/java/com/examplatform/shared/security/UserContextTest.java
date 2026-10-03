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

package com.examplatform.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("UserContext Unit Tests")
class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("set, get, and clear operations")
    class SetGetClear {

        @Test
        @DisplayName("Returns null values when context is empty")
        void emptyContext() {
            assertThat(UserContext.get()).isNull();
            assertThat(UserContext.getUserId()).isNull();
            assertThat(UserContext.getEmail()).isNull();
            assertThat(UserContext.getTenantId()).isNull();
            assertThat(UserContext.getRoles()).isEmpty();
            assertThat(UserContext.isAuthenticated()).isFalse();
            assertThat(UserContext.hasRole("ROLE_ADMIN")).isFalse();
            assertThat(UserContext.hasAnyRole("ADMIN", "USER")).isFalse();
        }

        @Test
        @DisplayName("Stores and retrieves CurrentUser record accurately")
        void storeAndRetrieve() {
            UUID userId = UUID.randomUUID();
            UserContext.set(userId, "admin@exam.gov.in", "tenant-nta", Set.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));

            assertThat(UserContext.isAuthenticated()).isTrue();
            assertThat(UserContext.getUserId()).isEqualTo(userId);
            assertThat(UserContext.getRequiredUserId()).isEqualTo(userId);
            assertThat(UserContext.getEmail()).isEqualTo("admin@exam.gov.in");
            assertThat(UserContext.getTenantId()).isEqualTo("tenant-nta");
            assertThat(UserContext.getRoles()).containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_ADMIN");
            assertThat(UserContext.hasRole("SUPER_ADMIN")).isTrue();
            assertThat(UserContext.hasRole("ROLE_SUPER_ADMIN")).isTrue();
            assertThat(UserContext.hasAnyRole("EXAM_CONTROLLER", "SUPER_ADMIN")).isTrue();
            assertThat(UserContext.hasRole("CANDIDATE")).isFalse();
        }

        @Test
        @DisplayName("Throws IllegalStateException on getRequiredUserId when unauthenticated")
        void throwsOnRequiredUserId() {
            assertThatThrownBy(UserContext::getRequiredUserId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No authenticated user present in UserContext");
        }

        @Test
        @DisplayName("Clears the context completely")
        void clearsContext() {
            UserContext.set(UUID.randomUUID(), "test@exam.gov.in", "default", Set.of("ROLE_CANDIDATE"));
            UserContext.clear();

            assertThat(UserContext.get()).isNull();
            assertThat(UserContext.isAuthenticated()).isFalse();
        }
    }

    @Nested
    @DisplayName("Thread isolation & Virtual Thread inheritance")
    class ThreadPropagation {

        @Test
        @DisplayName("Inherits UserContext in child thread and supports isolation")
        void childThreadInheritance() throws InterruptedException {
            UUID parentUserId = UUID.randomUUID();
            UserContext.set(parentUserId, "parent@exam.gov.in", "tenant-1", Set.of("ROLE_ADMIN"));

            AtomicReference<UUID> childObservedId = new AtomicReference<>();
            AtomicReference<String> childObservedTenant = new AtomicReference<>();

            Thread child = Thread.ofVirtual().start(() -> {
                childObservedId.set(UserContext.getUserId());
                childObservedTenant.set(UserContext.getTenantId());
            });
            child.join();

            assertThat(childObservedId.get()).isEqualTo(parentUserId);
            assertThat(childObservedTenant.get()).isEqualTo("tenant-1");
        }
    }
}
