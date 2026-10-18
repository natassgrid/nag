// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuickCheckItemDto(
        String question,
        List<String> options,
        String correctAnswer,
        String explanation
) implements Serializable {

    @JsonCreator
    public static QuickCheckItemDto create(
            @JsonProperty("question") String question,
            @JsonProperty("options") List<String> options,
            @JsonProperty("correctAnswer") String correctAnswer,
            @JsonProperty("answer") String answer,
            @JsonProperty("explanation") String explanation
    ) {
        return new QuickCheckItemDto(
                question,
                options,
                correctAnswer != null && !correctAnswer.isBlank() ? correctAnswer : answer,
                explanation
        );
    }

    @JsonProperty(value = "answer", access = JsonProperty.Access.READ_ONLY)
    public String getAnswer() {
        return correctAnswer;
    }
}


