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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.exception.ShiftComparabilityViolationException;
import com.examplatform.papergenerator.repository.PaperRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ShiftComparabilityService}.
 *
 * Validates: Requirements 8.9
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShiftComparabilityService")
class ShiftComparabilityServiceTest {

    @Mock
    PaperRepository paperRepository;

    @InjectMocks
    ShiftComparabilityService shiftComparabilityService;

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final int TOTAL_MARKS = 100;

    @Test
    @DisplayName("passes when difficulty difference is within 2% tolerance")
    void passesWithinTolerance() {
        Paper paper1 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-A")
                .difficultyScore(50.0)
                .status("GENERATED")
                .build();

        Paper paper2 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-B")
                .difficultyScore(51.5) // diff=1.5, relative=1.5% < 2%
                .status("GENERATED")
                .build();

        when(paperRepository.findByExamIdAndTenantId(EXAM_ID, null))
                .thenReturn(List.of(paper1, paper2));

        assertThatCode(() -> shiftComparabilityService.validateComparability(EXAM_ID, TOTAL_MARKS))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throws when difficulty difference exceeds 2% tolerance")
    void throwsWhenExceedsTolerance() {
        Paper paper1 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-A")
                .difficultyScore(50.0)
                .status("GENERATED")
                .build();

        Paper paper2 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-B")
                .difficultyScore(53.0) // diff=3.0, relative=3% > 2%
                .status("GENERATED")
                .build();

        when(paperRepository.findByExamIdAndTenantId(EXAM_ID, null))
                .thenReturn(List.of(paper1, paper2));

        assertThatThrownBy(() -> shiftComparabilityService.validateComparability(EXAM_ID, TOTAL_MARKS))
                .isInstanceOf(ShiftComparabilityViolationException.class)
                .hasMessageContaining("Shift comparability violated");
    }

    @Test
    @DisplayName("passes with single paper (no comparison needed)")
    void passesWithSinglePaper() {
        Paper paper1 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-A")
                .difficultyScore(50.0)
                .status("GENERATED")
                .build();

        when(paperRepository.findByExamIdAndTenantId(EXAM_ID, null))
                .thenReturn(List.of(paper1));

        assertThatCode(() -> shiftComparabilityService.validateComparability(EXAM_ID, TOTAL_MARKS))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("passes at exact boundary (2% relative difference)")
    void passesAtExactBoundary() {
        Paper paper1 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-A")
                .difficultyScore(50.0)
                .status("GENERATED")
                .build();

        Paper paper2 = Paper.builder()
                .examId(EXAM_ID)
                .shiftId("SHIFT-B")
                .difficultyScore(52.0) // diff=2.0, relative=2% == threshold (not >)
                .status("GENERATED")
                .build();

        when(paperRepository.findByExamIdAndTenantId(EXAM_ID, null))
                .thenReturn(List.of(paper1, paper2));

        assertThatCode(() -> shiftComparabilityService.validateComparability(EXAM_ID, TOTAL_MARKS))
                .doesNotThrowAnyException();
    }
}
