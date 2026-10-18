// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.shared.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    String targetExam,
    UUID enrollmentId,
    UUID examId
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
        String subtopicWiseBreakdown,
        String practiceSetSource,
        String targetExam
    ) {
        this(sessionId, candidateId, practiceSetId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks,
                topicWiseBreakdown, subtopicWiseBreakdown, practiceSetSource, targetExam, null, null);
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
        String topicWiseBreakdown,
        String subtopicWiseBreakdown
    ) {
        this(sessionId, candidateId, practiceSetId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks,
                topicWiseBreakdown, subtopicWiseBreakdown, null, null, null, null);
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
        this(sessionId, candidateId, practiceSetId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks,
                topicWiseBreakdown, null, null, null, null, null);
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
        this(sessionId, candidateId, null, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks,
                topicWiseBreakdown, null, null, null, null, null);
    }
}
