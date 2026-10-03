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
import com.tngtech.archunit.library.GeneralCodingRules;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * ArchUnit rules enforcing production coding standards and best practices.
 */
public final class CodingConventionRules {

    private CodingConventionRules() {
    }

    /**
     * Standard streams (System.out / System.err) should not be used for logging.
     */
    public static final ArchRule NO_SYSTEM_OUT_OR_ERR =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                    .because("Production code must use SLF4J loggers instead of System.out or System.err")
                    .allowEmptyShould(true);

    /**
     * Exception stack traces should not be printed directly via printStackTrace().
     */
    public static final ArchRule NO_PRINT_STACK_TRACE =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
                    .because("Java Util Logging is forbidden; SLF4J/Logback must be used")
                    .allowEmptyShould(true);

    /**
     * Generic exceptions like java.lang.Throwable should not be thrown.
     */
    public static final ArchRule NO_GENERIC_EXCEPTIONS =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
                    .because("Methods should throw specific, well-defined domain or runtime exceptions")
                    .allowEmptyShould(true);

    /**
     * Field injection with @Autowired is discouraged; constructor injection is required.
     */
    public static final ArchRule NO_FIELD_INJECTION =
            fields().should().notBeAnnotatedWith(Autowired.class)
                    .because("Constructor injection should always be used over @Autowired field injection for immutability and testability")
                    .allowEmptyShould(true);

    /**
     * Deprecated APIs should not be introduced or used in new code without justification.
     */
    public static final ArchRule NO_DEPRECATED_API_USAGE =
            GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED
                    .because("Deprecated APIs should be avoided in favor of current framework standards")
                    .allowEmptyShould(true);
}
