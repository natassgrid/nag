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

package com.examplatform.shared.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successWithData_hasCorrectStatusAndData() {
        ApiResponse<String> response = ApiResponse.success("hello", "OK");

        assertEquals("success", response.getStatus());
        assertEquals("hello", response.getData());
        assertEquals("OK", response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void successWithoutData_hasNullData() {
        ApiResponse<String> response = ApiResponse.success("No content");

        assertEquals("success", response.getStatus());
        assertNull(response.getData());
    }

    @Test
    void errorResponse_hasErrorStatusAndNullData() {
        ApiResponse<Object> response = ApiResponse.error("Something went wrong");

        assertEquals("error", response.getStatus());
        assertNull(response.getData());
        assertEquals("Something went wrong", response.getMessage());
    }

    @Test
    void timestampIsAlwaysPopulated() {
        ApiResponse<Void> response = ApiResponse.error("err");
        assertNotNull(response.getTimestamp());
    }
}
