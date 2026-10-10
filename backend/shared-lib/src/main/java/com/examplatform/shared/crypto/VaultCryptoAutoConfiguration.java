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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.shared.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.core.VaultTemplate;

/**
 * Auto-configuration for HSM/Vault-backed cryptographic operations.
 * Automatically provides {@link VaultCryptoService} when {@link VaultTemplate} is present.
 */
@AutoConfiguration
@ConditionalOnClass(VaultTemplate.class)
public class VaultCryptoAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(VaultCryptoService.class)
    @ConditionalOnBean(VaultTemplate.class)
    public VaultCryptoService vaultCryptoService(VaultTemplate vaultTemplate) {
        return new VaultCryptoServiceImpl(vaultTemplate);
    }
}
