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

import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import static com.examplatform.shared.archunit.ArchitectureConstants.CONTROLLER_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.DOMAIN_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.DTO_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.REPOSITORY_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.SERVICE_PACKAGE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit rules verifying layer isolation and boundary constraints.
 */
public final class LayerArchitectureRules {

    private LayerArchitectureRules() {
    }

    /**
     * Controllers should never directly access repositories; interactions must go through the service layer.
     */
    public static final ArchRule CONTROLLERS_SHOULD_NOT_ACCESS_REPOSITORIES =
            noClasses().that().resideInAPackage(CONTROLLER_PACKAGE)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY_PACKAGE)
                    .because("Controllers must delegate data access to the service layer and not touch repositories directly")
                    .allowEmptyShould(true);

    /**
     * Repositories should only be accessed by the service layer, async consumers, gRPC services, config layer, or other repositories.
     */
    public static final ArchRule REPOSITORIES_SHOULD_ONLY_BE_ACCESSED_BY_SERVICES_OR_REPOSITORIES =
            classes().that().resideInAPackage(REPOSITORY_PACKAGE)
                    .should().onlyBeAccessed().byAnyPackage(
                            SERVICE_PACKAGE,
                            REPOSITORY_PACKAGE,
                            "..consumer..",
                            "..listener..",
                            "..kafka..",
                            "..messaging..",
                            "..grpc..",
                            "..event..",
                            "..config..",
                            "..batch..",
                            "..runner..",
                            "..support..",
                            "..ledger..",
                            "..adapter..",
                            "..ai..",
                            "com.examplatform.shared.."
                    )
                    .because("Repositories are internal persistence layer components")
                    .allowEmptyShould(true);

    /**
     * Domain entities (classes annotated with @Entity or @Table) must reside in the domain or entity package.
     */
    public static final ArchRule ENTITIES_MUST_RESIDE_IN_DOMAIN_PACKAGE =
            classes().that().areAnnotatedWith(Entity.class)
                    .or().areAnnotatedWith(Table.class)
                    .should().resideInAnyPackage(DOMAIN_PACKAGE, "..entity..", "..model..", "..ai.batch..", "..ai..")
                    .because("JPA domain entities must reside in domain, entity, or model packages")
                    .allowEmptyShould(true);

    /**
     * Domain entities should not depend on controllers or web components.
     */
    public static final ArchRule ENTITIES_SHOULD_NOT_DEPEND_ON_CONTROLLERS =
            noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
                    .because("Domain models must remain decoupled from HTTP/REST presentation layers")
                    .allowEmptyShould(true);

    /**
     * DTOs should not be annotated with JPA entity annotations.
     */
    public static final ArchRule DTOS_MUST_NOT_BE_ENTITIES =
            noClasses().that().resideInAPackage(DTO_PACKAGE)
                    .should().beAnnotatedWith(Entity.class)
                    .orShould().beAnnotatedWith(Table.class)
                    .because("DTOs are external data transfer contracts and must not be mapped JPA entities")
                    .allowEmptyShould(true);
}
