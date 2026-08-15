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

package com.examplatform.questionbank.crypto;

import com.examplatform.questionbank.service.VaultCryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Static holder for {@link VaultCryptoService} and the encryption feature flag.
 * Bridges Spring-managed beans into JPA {@link jakarta.persistence.AttributeConverter}
 * instances, which are not Spring-managed by default.
 */
@Component
public class VaultCryptoServiceHolder {

    private static VaultCryptoService instance;
    private static boolean encryptionEnabled;

    public VaultCryptoServiceHolder(
            VaultCryptoService service,
            @Value("${app.encryption.enabled:false}") boolean encryptionEnabled) {
        VaultCryptoServiceHolder.instance = service;
        VaultCryptoServiceHolder.encryptionEnabled = encryptionEnabled;
    }

    public static VaultCryptoService getInstance() {
        return instance;
    }

    public static boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
}
