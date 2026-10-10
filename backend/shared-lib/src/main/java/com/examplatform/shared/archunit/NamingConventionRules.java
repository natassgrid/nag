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
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.examplatform.shared.archunit.ArchitectureConstants.CONFIG_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.CONTROLLER_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.ERROR_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.REPOSITORY_PACKAGE;
import static com.examplatform.shared.archunit.ArchitectureConstants.SERVICE_PACKAGE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * ArchUnit rules verifying standard class naming conventions.
 */
public final class NamingConventionRules {

    private NamingConventionRules() {
    }

    /**
     * Classes annotated with @RestController or @Controller should have names ending in 'Controller' or 'Resource'.
     */
    public static final ArchRule CONTROLLERS_SHOULD_BE_NAMED_PROPERLY =
            classes().that().areAnnotatedWith(RestController.class)
                    .or().areAnnotatedWith(Controller.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .orShould().haveSimpleNameEndingWith("Resource")
                    .because("REST controllers and web controllers should follow standard naming suffixes")
                    .allowEmptyShould(true);

    /**
     * Classes residing in controller packages should be named appropriately.
     */
    public static final ArchRule CONTROLLER_PACKAGE_CLASSES_NAMING =
            classes().that().resideInAPackage(CONTROLLER_PACKAGE)
                    .and().areTopLevelClasses()
                    .and().areNotAnonymousClasses()
                    .and().areNotEnums()
                    .and().areNotInterfaces()
                    .and().haveSimpleNameNotEndingWith("Builder")
                    .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>("have valid controller-layer suffix") {
                        @Override
                        public void check(com.tngtech.archunit.core.domain.JavaClass item, com.tngtech.archunit.lang.ConditionEvents events) {
                            String name = item.getSimpleName();
                            boolean ok = name.endsWith("Controller") || name.endsWith("Resource")
                                    || name.endsWith("Advice") || name.endsWith("ExceptionHandler")
                                    || name.endsWith("Helper") || name.endsWith("Aspect");
                            if (!ok) {
                                events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(item,
                                        item.getName() + " does not match accepted controller suffixes"));
                            }
                        }
                    })
                    .because("Classes in controller package should clearly reflect their web responsibility in their name")
                    .allowEmptyShould(true);

    /**
     * Classes annotated with @Service should reside in a service/engine/validation package or end with service-like suffixes.
     */
    public static final ArchRule SERVICES_SHOULD_BE_NAMED_PROPERLY =
            classes().that().areAnnotatedWith(Service.class)
                    .should().resideInAnyPackage(SERVICE_PACKAGE, "..validation..", "..engine..", "..pipeline..", "..ledger..", "..adapter..", "..client..", "..kafka..", "..consumer..", "..listener..", "..messaging..")
                    .orShould().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("ServiceImpl")
                    .orShould().haveSimpleNameEndingWith("Handler")
                    .orShould().haveSimpleNameEndingWith("Processor")
                    .orShould().haveSimpleNameEndingWith("Manager")
                    .orShould().haveSimpleNameEndingWith("Enricher")
                    .orShould().haveSimpleNameEndingWith("Randomizer")
                    .orShould().haveSimpleNameEndingWith("Parser")
                    .orShould().haveSimpleNameEndingWith("Pipeline")
                    .orShould().haveSimpleNameEndingWith("Validator")
                    .orShould().haveSimpleNameEndingWith("Task")
                    .orShould().haveSimpleNameEndingWith("Worker")
                    .orShould().haveSimpleNameEndingWith("Adapter")
                    .orShould().haveSimpleNameEndingWith("Client")
                    .orShould().haveSimpleNameEndingWith("Consumer")
                    .orShould().haveSimpleNameEndingWith("Listener")
                    .orShould().haveSimpleNameEndingWith("Producer")
                    .orShould().haveSimpleNameEndingWith("Runner")
                    .orShould().haveSimpleNameEndingWith("Builder")
                    .orShould().haveSimpleNameEndingWith("Factory")
                    .orShould().haveSimpleNameEndingWith("Helper")
                    .because("Spring @Service beans should follow standard service naming conventions")
                    .allowEmptyShould(true);

    /**
     * Spring Data Repositories or classes annotated with @Repository should end with 'Repository' or 'Custom'.
     */
    public static final ArchRule REPOSITORIES_SHOULD_BE_NAMED_PROPERLY =
            classes().that().areAnnotatedWith(Repository.class)
                    .or().areAssignableTo("org.springframework.data.repository.Repository")
                    .should().haveSimpleNameEndingWith("Repository")
                    .orShould().haveSimpleNameEndingWith("RepositoryImpl")
                    .orShould().haveSimpleNameEndingWith("Custom")
                    .orShould().haveSimpleNameEndingWith("CustomImpl")
                    .because("Persistence repository interfaces and implementations must end with 'Repository' or 'Custom'")
                    .allowEmptyShould(true);

    /**
     * Configuration classes annotated with @Configuration should reside in config packages and be named appropriately.
     */
    public static final ArchRule CONFIG_CLASSES_SHOULD_BE_NAMED_PROPERLY =
            classes().that().areAnnotatedWith(Configuration.class)
                    .should().resideInAPackage(CONFIG_PACKAGE)
                    .orShould().haveSimpleNameEndingWith("Config")
                    .orShould().haveSimpleNameEndingWith("Configuration")
                    .orShould().haveSimpleNameEndingWith("AutoConfiguration")
                    .because("Spring configuration classes must be identified with 'Config' or 'Configuration' suffix")
                    .allowEmptyShould(true);

    /**
     * Exception classes extending Throwable / Exception in error package should end with 'Exception' or 'Error'.
     */
    public static final ArchRule EXCEPTIONS_SHOULD_BE_NAMED_PROPERLY =
            classes().that().areAssignableTo(Exception.class)
                    .and().resideInAPackage(ERROR_PACKAGE)
                    .should().haveSimpleNameEndingWith("Exception")
                    .orShould().haveSimpleNameEndingWith("Error")
                    .because("Custom exception classes must follow standard Java exception naming")
                    .allowEmptyShould(true);
}
