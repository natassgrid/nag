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

package com.examplatform.shared.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.examplatform.shared",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class}
)
class SharedLibArchitectureTest {

    @ArchTest
    static final ArchRule sharedLibMustNotDependOnMicroservices =
            SharedLibraryRules.SHARED_LIB_MUST_NOT_DEPEND_ON_MICROSERVICES;

    @ArchTest
    static final ArchRule noSystemOutOrErr =
            CodingConventionRules.NO_SYSTEM_OUT_OR_ERR;

    @ArchTest
    static final ArchRule noGenericExceptions =
            CodingConventionRules.NO_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule noFieldInjection =
            CodingConventionRules.NO_FIELD_INJECTION;

    @ArchTest
    static final ArchRule exceptionsShouldBeNamedProperly =
            NamingConventionRules.EXCEPTIONS_SHOULD_BE_NAMED_PROPERLY;
}
