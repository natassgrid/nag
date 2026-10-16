// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeakTopicItemDto(
    @JsonAlias({"topic"})
    String topicName,
    double currentAccuracy,
    @JsonAlias({"subtopics"})
    List<String> subtopicsToRevise,
    @JsonAlias({"subject"})
    String subjectName,
    Long subtopicId
) {
    public WeakTopicItemDto(String topicName, double currentAccuracy, List<String> subtopicsToRevise) {
        this(topicName, currentAccuracy, subtopicsToRevise, null, null);
    }
}
