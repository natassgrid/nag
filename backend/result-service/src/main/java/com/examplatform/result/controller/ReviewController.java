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

package com.examplatform.result.controller;

import com.examplatform.result.dto.ExamReviewResponse;
import com.examplatform.result.dto.ReviewQuestionDto;
import com.examplatform.result.dto.ReviewOptionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for post-exam question review.
 * Returns candidate's question-by-question performance with solutions and peer benchmarks.
 *
 * Validates: SPEC-UI3 (Review API Endpoint)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ReviewController {

    /**
     * Returns the full post-exam review for a candidate in an exam.
     * Includes each question, candidate's answer, correct answer, explanation, and peer statistics.
     *
     * Access: CANDIDATE role only (own results), EXAM_CONTROLLER can view any.
     *
     * @param candidateId the candidate UUID
     * @param examId      the exam UUID
     * @param auth        authentication principal
     * @return the exam review response
     */
    @GetMapping("/{candidateId}/review")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EXAM_CONTROLLER')")
    public ResponseEntity<ExamReviewResponse> getExamReview(
            @PathVariable UUID candidateId,
            @RequestParam UUID examId,
            Authentication auth) {

        log.info("GET exam review for candidate={}, exam={}", candidateId, examId);

        // TODO: In full implementation, fetch from:
        // 1. evaluation-service — candidate's responses + scores per question
        // 2. question-bank-service — question content, options, explanations
        // 3. QuestionAnalyticsService — peer accuracy per question
        // For now, return a structured stub to establish the API contract.

        ExamReviewResponse reviewResponse = ExamReviewResponse.builder()
                .examId(examId)
                .candidateId(candidateId)
                .questions(buildStubReviewQuestions(examId, candidateId))
                .build();

        return ResponseEntity.ok(reviewResponse);
    }

    /**
     * Public QR code verification endpoint.
     * Returns redacted result information for a given QR verification code.
     * Rate-limited at API gateway level (60 req/min per IP).
     *
     * @param code the QR verification UUID token from the scorecard
     * @return redacted result or 404 if code not found
     */
    @GetMapping("/verify")
    public ResponseEntity<java.util.Map<String, Object>> verifyScorecard(@RequestParam String code) {
        log.info("GET QR verification for code={}", code);

        // TODO: In full implementation, look up Result by qrVerificationCode and return redacted fields
        // For now, return a stub response to establish the API contract.
        return ResponseEntity.ok(java.util.Map.of(
                "valid", false,
                "message", "QR verification endpoint active — implementation pending result-service integration",
                "code", code
        ));
    }

    /**
     * Builds stub review questions for API contract establishment.
     * In full implementation, this would be replaced by real cross-service data.
     */
    private List<ReviewQuestionDto> buildStubReviewQuestions(UUID examId, UUID candidateId) {
        return List.of(
            ReviewQuestionDto.builder()
                .questionId(UUID.nameUUIDFromBytes(("q1-" + examId).getBytes()))
                .questionNumber(1)
                .content("Sample question content with $$\\frac{a}{b}$$ formula.")
                .subject("Physics")
                .topic("Thermodynamics")
                .difficulty("MEDIUM")
                .bloomsLevel("APPLY")
                .options(List.of(
                    ReviewOptionDto.builder().id("opt-a").text("Option A").isCorrect(true).build(),
                    ReviewOptionDto.builder().id("opt-b").text("Option B").isCorrect(false).build(),
                    ReviewOptionDto.builder().id("opt-c").text("Option C").isCorrect(false).build(),
                    ReviewOptionDto.builder().id("opt-d").text("Option D").isCorrect(false).build()
                ))
                .candidateSelectedOptionIds(List.of("opt-b"))
                .isCorrect(false)
                .marksAwarded(-0.25)
                .timeSpentMs(83000L)
                .peerAccuracyPct(42.3)
                .explanation("The correct answer is A because $$\\frac{a}{b}$$ represents the ratio.")
                .build()
        );
    }
}
