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

import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.api.ExamPlatformProblemDetail;
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.auth.TokenResponse;
import com.examplatform.shared.config.SystemConfigChangeEvent;
import com.examplatform.shared.entity.BaseEntity;
import com.examplatform.shared.entity.NumericBaseEntity;
import com.examplatform.shared.error.ProblemDetailBuilder;
import com.examplatform.shared.lifecycle.EvaluationState;
import com.examplatform.shared.lifecycle.PaperState;
import com.examplatform.shared.lifecycle.QuestionState;
import com.examplatform.shared.lifecycle.SessionState;
import com.examplatform.shared.lifecycle.TranslationState;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.examplatform.shared.tenant.TenantContext;
import com.examplatform.shared.util.UuidV7Generator;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Boot 4.x &amp; GraalVM AOT Runtime Hints Registrar.
 * <p>
 * Registers reflection, resource, and serialization metadata for shared domain models,
 * DTOs, lifecycle enums, Jackson serialization, UUID v7 generation, and Spring Security tokens
 * to ensure seamless ahead-of-time (AOT) compilation into native binaries.
 */
public class GraalVmRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    private static final List<Class<?>> SHARED_REFLECTION_TYPES = Arrays.asList(
            // API Response and Error models
            ApiResponse.class,
            ExamPlatformProblemDetail.class,
            ProblemDetailBuilder.class,

            // Auth & Token models
            TokenResponse.class,

            // Lifecycle Enums
            AuditEventType.class,
            EvaluationState.class,
            PaperState.class,
            QuestionState.class,
            SessionState.class,
            TranslationState.class,

            // Domain Events & Messaging
            GenericDomainEvent.class,
            SystemConfigChangeEvent.class,

            // Tenant and Context models
            TenantContext.class,
            BaseEntity.class,
            NumericBaseEntity.class,

            // Utilities & UUID generators
            UuidV7Generator.class
    );

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 1. Register reflection for shared domain models and DTOs
        for (Class<?> clazz : SHARED_REFLECTION_TYPES) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.INVOKE_PUBLIC_METHODS
            );
        }

        // 2. Register reflection for third-party libraries if present on classpath
        registerClassIfPresent(hints, classLoader, "com.fasterxml.uuid.Generators",
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        registerClassIfPresent(hints, classLoader, "com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator",
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        registerClassIfPresent(hints, classLoader, "org.postgresql.util.PGobject",
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        registerClassIfPresent(hints, classLoader, "com.pgvector.PGvector",
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        // 3. Register common resource patterns
        hints.resources().registerPattern("META-INF/spring/*");
        hints.resources().registerPattern("db/migration/*");
        hints.resources().registerPattern("application*.yml");
        hints.resources().registerPattern("application*.yaml");
        hints.resources().registerPattern("application*.properties");
        hints.resources().registerPattern("*.proto");
    }

    private void registerClassIfPresent(RuntimeHints hints, ClassLoader classLoader, String className, MemberCategory... categories) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            hints.reflection().registerType(clazz, categories);
        } catch (ClassNotFoundException ignored) {
            // Class is not available in the current classloader, safely skip
        }
    }
}
