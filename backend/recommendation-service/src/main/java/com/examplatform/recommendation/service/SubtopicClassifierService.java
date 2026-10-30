// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.SubtopicClassificationResult;
import com.examplatform.shared.constants.SharedConstants;
import org.springframework.stereotype.Service;

/**
 * Deterministic 3-tier sub-topic classifier implementing exact business rules:
 * <ul>
 *   <li>Accuracy &lt; 50.0% OR Skip Rate &gt;= 40.0% &rarr; CONCEPTUAL_GAP</li>
 *   <li>Accuracy 50.0% - 75.0% (inclusive) &rarr; APPLICATION_GAP</li>
 *   <li>Accuracy &gt; 75.0% &rarr; PROFICIENT (Pace ratio modifier applied: &gt; 1.25 &rarr; TIMED_SPRINT_DRILLS, &lt;= 1.25 &rarr; ADVANCED_TIER_QUESTIONS)</li>
 * </ul>
 */
@Service
public class SubtopicClassifierService {

    public static final String ACTION_CONCEPTUAL = "CORE_CONCEPTUAL_MICRO_LESSONS";
    public static final String ACTION_APPLICATION = "PRACTICE_WORKSHEET_MEDIUM";
    public static final String ACTION_SPEED_SPRINT = "TIMED_SPRINT_DRILLS";
    public static final String ACTION_ADVANCED_TIER = "ADVANCED_TIER_QUESTIONS";

    public SubtopicClassificationResult classify(
            Long subjectId,
            String subjectName,
            Long topicId,
            String topicName,
            Long subtopicId,
            String subtopicName,
            double accuracyPercent,
            double paceRatio,
            double skipRatePercent) {

        PerformanceBracket bracket;
        boolean slowPace = paceRatio > PerformanceBracket.PACE_RATIO_SLOW_THRESHOLD;
        String action;
        String note;

        // Skip rate override: Skip Rate >= 40% classifies as Conceptual Gap regardless of accuracy
        if (skipRatePercent >= PerformanceBracket.HIGH_SKIP_RATE_THRESHOLD) {
            bracket = PerformanceBracket.CONCEPTUAL_GAP;
            action = ACTION_CONCEPTUAL;
            note = String.format("High skip rate (%.1f%% >= 40.0%%): Core conceptual micro-lessons and foundational formulas required.", skipRatePercent);
        } else if (accuracyPercent < PerformanceBracket.CONCEPTUAL_ACCURACY_THRESHOLD) {
            bracket = PerformanceBracket.CONCEPTUAL_GAP;
            action = ACTION_CONCEPTUAL;
            note = String.format("Low accuracy (%.1f%% < 50.0%%): Foundational theory and solved derivations assigned. Advance practice locked.", accuracyPercent);
        } else if (accuracyPercent <= PerformanceBracket.APPLICATION_ACCURACY_THRESHOLD) {
            bracket = PerformanceBracket.APPLICATION_GAP;
            action = ACTION_APPLICATION;
            note = String.format("Moderate accuracy (%.1f%% in [50.0%%, 75.0%%]): Skip basic theory; targeted medium practice worksheets and shortcut elimination assigned.", accuracyPercent);
        } else {
            bracket = PerformanceBracket.PROFICIENT;
            if (slowPace) {
                action = ACTION_SPEED_SPRINT;
                note = String.format("High accuracy (%.1f%% > 75.0%%) with slow pacing (pace ratio %.2f > 1.25): Scheduled timed sprint drills for speed optimization.", accuracyPercent, paceRatio);
            } else {
                action = ACTION_ADVANCED_TIER;
                note = String.format("High accuracy (%.1f%% > 75.0%%) with optimal pacing (pace ratio %.2f <= 1.25): Assigned advanced-tier multi-correct and assertion questions.", accuracyPercent, paceRatio);
            }
        }

        return new SubtopicClassificationResult(
                subjectId,
                subjectName != null ? subjectName : SharedConstants.GENERAL,
                topicId,
                topicName != null ? topicName : SharedConstants.GENERAL,
                subtopicId,
                subtopicName,
                bracket,
                accuracyPercent,
                paceRatio,
                skipRatePercent,
                slowPace,
                action,
                note
        );
    }

    public SubtopicClassificationResult classify(LearnerSubtopicPerformance perf) {
        return classify(
                perf.getSubjectId(),
                perf.getSubjectName(),
                perf.getTopicId(),
                perf.getTopicName(),
                perf.getSubtopicId(),
                perf.getSubtopicName(),
                perf.getAvgAccuracyPercent(),
                perf.getAvgPaceRatio(),
                perf.getAvgSkipRatePercent()
        );
    }
}
