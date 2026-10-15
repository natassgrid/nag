// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.shared.event;

import java.io.Serializable;
import java.util.UUID;

public record PracticeSessionCompletedEvent(
    UUID sessionId,
    UUID candidateId,
    UUID practiceSetId,
    int correctCount,
    int incorrectCount,
    int skippedCount,
    int obtainedMarks,
    int totalMarks,
    String topicWiseBreakdown,
    String subtopicWiseBreakdown
) implements Serializable {
    public PracticeSessionCompletedEvent(
        UUID sessionId,
        UUID candidateId,
        UUID practiceSetId,
        int correctCount,
        int incorrectCount,
        int skippedCount,
        int obtainedMarks,
        int totalMarks,
        String topicWiseBreakdown
    ) {
        this(sessionId, candidateId, practiceSetId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks, topicWiseBreakdown, null);
    }
}
