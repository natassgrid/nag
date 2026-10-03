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

package com.examplatform.shared.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Standard JSON-RPC 2.0 Response envelope for inter-service communication.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
        String jsonrpc,
        Object result,
        JsonRpcError error,
        Object id
) {
    public static JsonRpcResponse success(Object id, Object result) {
        return new JsonRpcResponse("2.0", result, null, id);
    }

    public static JsonRpcResponse error(Object id, int code, String message) {
        return new JsonRpcResponse("2.0", null, new JsonRpcError(code, message, null), id);
    }

    public record JsonRpcError(
            int code,
            String message,
            Object data
    ) {}
}
