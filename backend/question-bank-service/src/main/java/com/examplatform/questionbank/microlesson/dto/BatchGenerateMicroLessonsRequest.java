// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchGenerateMicroLessonsRequest(
        Long subjectId,
        Long topicId,
        List<Long> subtopicIds,
        String targetExam, // defaults to JEE_MAIN
        Integer maxCount
) implements Serializable {}
