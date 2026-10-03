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
 * GNU标识 Affero General Public License for more details.
 */

package com.examplatform.shared.archunit;

import com.tngtech.archunit.lang.ArchRule;

import static com.examplatform.shared.archunit.ArchitectureConstants.SHARED_PACKAGE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit rules verifying shared library boundaries.
 */
public final class SharedLibraryRules {

    private SharedLibraryRules() {
    }

    /**
     * Shared library must be domain-agnostic and never depend on microservice-specific packages.
     */
    public static final ArchRule SHARED_LIB_MUST_NOT_DEPEND_ON_MICROSERVICES =
            noClasses().that().resideInAPackage(SHARED_PACKAGE)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.examplatform.identity..",
                            "com.examplatform.candidate..",
                            "com.examplatform.delivery..",
                            "com.examplatform.evaluation..",
                            "com.examplatform.result..",
                            "com.examplatform.audit..",
                            "com.examplatform.notification..",
                            "com.examplatform.papergenerator..",
                            "com.examplatform.questionbank..",
                            "com.examplatform.response..",
                            "com.examplatform.admin..",
                            "com.examplatform.analytics..",
                            "com.examplatform.asset..",
                            "com.examplatform.examination.."
                    )
                    .because("Shared-lib must remain generic and decoupled from individual microservice implementations")
                    .allowEmptyShould(true);
}
