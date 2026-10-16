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
    String subtopicWiseBreakdown,
    String practiceSetSource,
    String targetExam
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
        String topicWiseBreakdown,
        String subtopicWiseBreakdown
    ) {
        this(sessionId, candidateId, practiceSetId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks, topicWiseBreakdown, subtopicWiseBreakdown, null, null);
    }

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
        this(sessionId, candidateId, practiceSetId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks, topicWiseBreakdown, null, null, null);
    }

    public PracticeSessionCompletedEvent(
        UUID sessionId,
        UUID candidateId,
        int correctCount,
        int incorrectCount,
        int skippedCount,
        int obtainedMarks,
        int totalMarks,
        String topicWiseBreakdown
    ) {
        this(sessionId, candidateId, null, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks, topicWiseBreakdown, null, null, null);
    }
}
