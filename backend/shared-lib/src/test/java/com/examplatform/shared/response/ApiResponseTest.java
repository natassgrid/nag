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

package com.examplatform.shared.response;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponse")
class ApiResponseTest {

    @Nested
    @DisplayName("success factory")
    class SuccessFactory {

        @Test
        @DisplayName("wraps data with SUCCESS status")
        void wrapsDataWithSuccessStatus() {
            var response = ApiResponse.success("hello");
            assertAll(
                () -> assertThat(response.getStatus()).isEqualTo("SUCCESS"),
                () -> assertThat(response.getData()).isEqualTo("hello"),
                () -> assertThat(response.getMessage()).isNull(),
                () -> assertThat(response.getTimestamp()).isNotNull()
            );
        }

        @Test
        @DisplayName("includes message when provided")
        void includesMessageWhenProvided() {
            var response = ApiResponse.success(42, "Created successfully");
            assertAll(
                () -> assertThat(response.getMessage()).isEqualTo("Created successfully"),
                () -> assertThat(response.getData()).isEqualTo(42)
            );
        }

        @ParameterizedTest(name = "data={0}")
        @ValueSource(strings = {"value1", "value2", "value3"})
        @DisplayName("preserves arbitrary string data")
        void preservesArbitraryStringData(String value) {
            assertThat(ApiResponse.success(value).getData()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("error factory")
    class ErrorFactory {

        @Test
        @DisplayName("sets ERROR status with message and null data")
        void setsErrorStatus() {
            var response = ApiResponse.error("Something went wrong");
            assertAll(
                () -> assertThat(response.getStatus()).isEqualTo("ERROR"),
                () -> assertThat(response.getMessage()).isEqualTo("Something went wrong"),
                () -> assertThat(response.getData()).isNull(),
                () -> assertThat(response.getTimestamp()).isNotNull()
            );
        }
    }
}
