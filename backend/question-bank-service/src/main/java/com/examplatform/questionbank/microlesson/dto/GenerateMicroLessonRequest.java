// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import java.io.Serializable;

public record GenerateMicroLessonRequest(
        Long subjectId,
        String subject,
        Long topicId,
        String topic,
        Long subtopicId,
        String subtopic,
        String targetExam,
        String referenceText,
        String promptNotes,
        String lessonType,
        String difficultyLevel,
        String language
) implements Serializable {

    public GenerateMicroLessonRequest(
            Long subjectId,
            String subject,
            Long topicId,
            String topic,
            Long subtopicId,
            String subtopic,
            String targetExam,
            String referenceText,
            String promptNotes
    ) {
        this(subjectId, subject, topicId, topic, subtopicId, subtopic, targetExam,
                referenceText, promptNotes, "CONCEPT", "CORE", "en");
    }
}
