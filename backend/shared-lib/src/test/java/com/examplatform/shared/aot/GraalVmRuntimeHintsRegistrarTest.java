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
import com.examplatform.shared.lifecycle.QuestionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class GraalVmRuntimeHintsRegistrarTest {

    @Test
    @DisplayName("Should register reflection hints for shared API models, lifecycle enums, and resources")
    void shouldRegisterReflectionAndResourceHints() {
        GraalVmRuntimeHintsRegistrar registrar = new GraalVmRuntimeHintsRegistrar();
        RuntimeHints hints = new RuntimeHints();

        registrar.registerHints(hints, getClass().getClassLoader());

        // Verify reflection registration for ApiResponse
        assertThat(RuntimeHintsPredicates.reflection().onType(ApiResponse.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(ExamPlatformProblemDetail.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(QuestionState.class)).accepts(hints);

        // Verify resource registration
        assertThat(RuntimeHintsPredicates.resource().forResource("application.yml")).accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("META-INF/spring/aot.factories")).accepts(hints);
    }
}
