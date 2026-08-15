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

package com.examplatform.asset.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that discovers and manages all available {@link StorageProvider} implementations.
 *
 * <p>Providers are auto-discovered via Spring dependency injection.
 * The active provider is selected based on application configuration.
 */
@Slf4j
@Component
public class StorageProviderRegistry {

    private final Map<String, StorageProvider> providers = new ConcurrentHashMap<>();

    public StorageProviderRegistry(List<StorageProvider> storageProviders) {
        for (StorageProvider provider : storageProviders) {
            providers.put(provider.name(), provider);
            log.info("Registered storage provider: {}", provider.name());
        }
        log.info("Total storage providers registered: {}", providers.size());
    }

    /**
     * Get a provider by name.
     *
     * @param name the provider name
     * @return the provider, or empty if not found
     */
    public Optional<StorageProvider> getProvider(String name) {
        return Optional.ofNullable(providers.get(name));
    }

    /**
     * Get a provider by name, throwing if not found.
     *
     * @param name the provider name
     * @return the provider
     * @throws IllegalArgumentException if provider not registered
     */
    public StorageProvider requireProvider(String name) {
        return getProvider(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Storage provider not found: '" + name + "'. Available: " + providers.keySet()));
    }

    /**
     * @return all registered provider names
     */
    public java.util.Set<String> availableProviders() {
        return providers.keySet();
    }
}
