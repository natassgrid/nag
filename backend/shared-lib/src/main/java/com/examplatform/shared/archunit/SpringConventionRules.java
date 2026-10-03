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

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import static com.examplatform.shared.archunit.ArchitectureConstants.CONTROLLER_PACKAGE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * ArchUnit rules for Spring Boot, MVC, Security, and Transaction conventions.
 */
public final class SpringConventionRules {

    private SpringConventionRules() {
    }

    /**
     * Controller methods must not be annotated with @Transactional (transactions belong in the service layer).
     */
    public static final ArchRule NO_TRANSACTIONAL_ON_CONTROLLER_METHODS =
            noMethods().that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
                    .should().beAnnotatedWith(Transactional.class)
                    .orShould().beAnnotatedWith("jakarta.transaction.Transactional")
                    .because("Database transaction boundaries belong in the Service or Persistence layer, not in Controllers")
                    .allowEmptyShould(true);

    /**
     * Configuration classes should provide bean definitions or standard configuration.
     */
    public static final ArchRule CONFIG_CLASSES_MUST_BE_PROPERLY_ANNOTATED =
            classes().that().haveSimpleNameEndingWith("Config")
                    .and().areTopLevelClasses()
                    .and().areNotInterfaces()
                    .and().areNotAnonymousClasses()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should().beAnnotatedWith(Configuration.class)
                    .orShould().beAnnotatedWith("org.springframework.boot.autoconfigure.SpringBootApplication")
                    .orShould().beAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
                    .because("Classes named *Config must be Spring @Configuration or configuration properties")
                    .allowEmptyShould(true);
}
