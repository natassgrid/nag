/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.shared.aot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.NativeDetector;

/**
 * Spring Boot 4.x Auto-configuration for GraalVM Native Image support.
 * <p>
 * Binds shared runtime hints across all microservices and provides native runtime detection diagnostics.
 */
@AutoConfiguration
@ImportRuntimeHints(GraalVmRuntimeHintsRegistrar.class)
public class GraalVmAutoConfiguration {

    @PostConstruct
    public void logRuntimeEnvironment() {
        if (NativeDetector.inNativeImage()) {
            System.out.println("🚀 Running inside GraalVM Native Image (AOT compiled with Spring Boot 4.x)");
        }
    }
}
