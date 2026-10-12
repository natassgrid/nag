// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.domain;

/**
 * Three-tier master performance classification bracket for rule-based DLP engine:
 * <ul>
 *   <li>CONCEPTUAL_GAP: Accuracy &lt; 50.0% or Skip Rate &gt;= 40.0%</li>
 *   <li>APPLICATION_GAP: Accuracy 50.0% - 75.0%</li>
 *   <li>PROFICIENT: Accuracy &gt; 75.0% (with pace ratio modifier for speed sprints vs advanced tier)</li>
 * </ul>
 */
public enum PerformanceBracket {
    CONCEPTUAL_GAP,
    APPLICATION_GAP,
    PROFICIENT;

    public static final double CONCEPTUAL_ACCURACY_THRESHOLD = 50.0;
    public static final double APPLICATION_ACCURACY_THRESHOLD = 75.0;
    public static final double HIGH_SKIP_RATE_THRESHOLD = 40.0;
    public static final double PACE_RATIO_SLOW_THRESHOLD = 1.25;
}
