/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidOptionValidator")
class ValidOptionValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass validation when text is present and imageUrl is null")
    void shouldPassWhenTextIsPresent() {
        QuestionOption option = QuestionOption.builder()
                .id("A")
                .text("Option text without image")
                .correct(true)
                .build();

        Set<ConstraintViolation<QuestionOption>> violations = validator.validate(option);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should pass validation when imageUrl is present and text is null")
    void shouldPassWhenImageUrlIsPresent() {
        QuestionOption option = QuestionOption.builder()
                .id("B")
                .imageUrl("https://cdn.examplatform.org/diagrams/opt-b.svg")
                .imageAltText("A triangle diagram")
                .correct(false)
                .build();

        Set<ConstraintViolation<QuestionOption>> violations = validator.validate(option);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should pass validation when both text and imageUrl are present")
    void shouldPassWhenBothTextAndImageArePresent() {
        QuestionOption option = QuestionOption.builder()
                .id("C")
                .text("Option with both")
                .imageUrl("https://cdn.examplatform.org/diagrams/opt-c.png")
                .imageAltText("Option C diagram")
                .correct(false)
                .build();

        Set<ConstraintViolation<QuestionOption>> violations = validator.validate(option);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should fail validation when neither text nor imageUrl is provided")
    void shouldFailWhenBothTextAndImageAreNull() {
        QuestionOption option = QuestionOption.builder()
                .id("D")
                .correct(false)
                .build();

        Set<ConstraintViolation<QuestionOption>> violations = validator.validate(option);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Option must have either text or an image URL");
    }

    @Test
    @DisplayName("should fail validation when text and imageUrl are both blank strings")
    void shouldFailWhenBothTextAndImageAreBlank() {
        QuestionOption option = QuestionOption.builder()
                .id("E")
                .text("   ")
                .imageUrl("   ")
                .correct(false)
                .build();

        Set<ConstraintViolation<QuestionOption>> violations = validator.validate(option);
        assertThat(violations).hasSize(1);
    }
}
