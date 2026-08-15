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

package com.examplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the WebAuthn / FIDO2 assertion from the client.
 * All byte-array fields are transmitted as Base64URL-encoded strings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnAssertionRequest {

    /** Base64URL-encoded credential ID. */
    @NotBlank
    private String credentialId;

    /** Base64URL-encoded authenticator data. */
    @NotBlank
    private String authenticatorData;

    /** Base64URL-encoded client data JSON. */
    @NotBlank
    private String clientDataJSON;

    /** Base64URL-encoded signature. */
    @NotBlank
    private String signature;

    /** Optional: Base64URL user handle (userId). */
    private String userHandle;
}
