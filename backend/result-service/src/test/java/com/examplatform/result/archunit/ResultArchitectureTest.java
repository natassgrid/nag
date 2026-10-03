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

package com.examplatform.result.archunit;

import com.examplatform.shared.archunit.CodingConventionRules;
import com.examplatform.shared.archunit.LayerArchitectureRules;
import com.examplatform.shared.archunit.NamingConventionRules;
import com.examplatform.shared.archunit.SpringConventionRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.examplatform.result",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class}
)
class ResultArchitectureTest {

    @ArchTest
    static final ArchRule controllersShouldNotAccessRepositories =
            LayerArchitectureRules.CONTROLLERS_SHOULD_NOT_ACCESS_REPOSITORIES;

    @ArchTest
    static final ArchRule repositoriesShouldOnlyBeAccessedByServicesOrRepositories =
            LayerArchitectureRules.REPOSITORIES_SHOULD_ONLY_BE_ACCESSED_BY_SERVICES_OR_REPOSITORIES;

    @ArchTest
    static final ArchRule entitiesMustResideInDomainPackage =
            LayerArchitectureRules.ENTITIES_MUST_RESIDE_IN_DOMAIN_PACKAGE;

    @ArchTest
    static final ArchRule entitiesShouldNotDependOnControllers =
            LayerArchitectureRules.ENTITIES_SHOULD_NOT_DEPEND_ON_CONTROLLERS;

    @ArchTest
    static final ArchRule dtosMustNotBeEntities =
            LayerArchitectureRules.DTOS_MUST_NOT_BE_ENTITIES;

    @ArchTest
    static final ArchRule controllersShouldBeNamedProperly =
            NamingConventionRules.CONTROLLERS_SHOULD_BE_NAMED_PROPERLY;

    @ArchTest
    static final ArchRule servicesShouldBeNamedProperly =
            NamingConventionRules.SERVICES_SHOULD_BE_NAMED_PROPERLY;

    @ArchTest
    static final ArchRule repositoriesShouldBeNamedProperly =
            NamingConventionRules.REPOSITORIES_SHOULD_BE_NAMED_PROPERLY;

    @ArchTest
    static final ArchRule configClassesShouldBeNamedProperly =
            NamingConventionRules.CONFIG_CLASSES_SHOULD_BE_NAMED_PROPERLY;

    @ArchTest
    static final ArchRule noTransactionalOnControllerMethods =
            SpringConventionRules.NO_TRANSACTIONAL_ON_CONTROLLER_METHODS;

    @ArchTest
    static final ArchRule noSystemOutOrErr =
            CodingConventionRules.NO_SYSTEM_OUT_OR_ERR;

    @ArchTest
    static final ArchRule noFieldInjection =
            CodingConventionRules.NO_FIELD_INJECTION;
}
