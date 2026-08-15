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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Validates that all papers for an exam are comparable in difficulty.
 * For any pair of papers, the relative difficulty difference must not exceed 2%.
 *
 * Validates: Requirements 8.9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftComparabilityService {

    private static final double MAX_RELATIVE_DIFFERENCE = 0.02;

    private final PaperRepository paperRepository;

    /**
     * Validates comparability across all papers generated for a given exam.
     * For any pair of papers, computes |difficultyScore_a - difficultyScore_b| / totalMarks.
     * Throws if the relative difference exceeds 2%.
     *
     * @param examId     the exam to validate papers for
     * @param totalMarks the total marks for the exam (used as denominator)
     * @throws ShiftComparabilityViolationException if any pair exceeds the tolerance
     */
    public void validateComparability(UUID examId, int totalMarks) {
        List<Paper> papers = paperRepository.findByExamIdAndTenantId(examId, null);

        if (papers.size() < 2) {
            log.debug("Less than 2 papers for exam={}, skipping comparability check", examId);
            return;
        }

        List<String> violations = new ArrayList<>();

        for (int i = 0; i < papers.size(); i++) {
            for (int j = i + 1; j < papers.size(); j++) {
                Paper a = papers.get(i);
                Paper b = papers.get(j);
                double diff = Math.abs(a.getDifficultyScore() - b.getDifficultyScore());
                double relativeDiff = diff / totalMarks;

                if (relativeDiff > MAX_RELATIVE_DIFFERENCE) {
                    String violation = String.format(
                            "Papers [%s] (shift=%s, score=%.2f) and [%s] (shift=%s, score=%.2f) " +
                                    "differ by %.4f (%.2f%%) — exceeds 2%% threshold",
                            a.getId(), a.getShiftId(), a.getDifficultyScore(),
                            b.getId(), b.getShiftId(), b.getDifficultyScore(),
                            diff, relativeDiff * 100);
                    violations.add(violation);
                }
            }
        }

        if (!violations.isEmpty()) {
            log.warn("Shift comparability violated for exam={}: {} violations found", examId, violations.size());
            throw new ShiftComparabilityViolationException(examId, violations);
        }

        log.info("Shift comparability validated for exam={} across {} papers", examId, papers.size());
    }
}
