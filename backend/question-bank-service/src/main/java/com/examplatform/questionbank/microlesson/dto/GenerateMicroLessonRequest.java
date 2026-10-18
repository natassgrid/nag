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
        String promptNotes
) implements Serializable {}
