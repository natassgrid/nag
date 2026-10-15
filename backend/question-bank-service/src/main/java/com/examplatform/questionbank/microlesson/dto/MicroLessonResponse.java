// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MicroLessonResponse(
        UUID id,
        Long subjectId,
        Long topicId,
        Long subtopicId,
        String subject,
        String topic,
        String subtopic,
        String targetExam,
        String coreConcept,
        String trap,
        List<String> cheatSheet,
        StepByStepBreakdownDto stepByStepBreakdown,
        String status,
        UUID reviewedBy,
        Instant reviewedAt,
        String reviewComments,
        int contentVersion,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {

    @JsonProperty("subtopicName")
    public String getSubtopicName() {
        return subtopic;
    }

    @JsonProperty("topicName")
    public String getTopicName() {
        return topic;
    }

    @JsonProperty("subjectName")
    public String getSubjectName() {
        return subject;
    }

    @JsonProperty("trapAlert")
    public String getTrapAlert() {
        return trap;
    }

    @JsonProperty("reviewerNotes")
    public String getReviewerNotes() {
        return reviewComments;
    }
}
