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
public record FormulaEntryDto(
        String name,
        String latex,
        String description,
        List<String> variables
) implements Serializable {

    @JsonCreator
    public static FormulaEntryDto create(
            @JsonProperty("name") String name,
            @JsonProperty("title") String title,
            @JsonProperty("latex") String latex,
            @JsonProperty("formula") String formula,
            @JsonProperty("description") String description,
            @JsonProperty("usage") String usage,
            @JsonProperty("variables") List<String> variables
    ) {
        return new FormulaEntryDto(
                name != null && !name.isBlank() ? name : title,
                latex != null && !latex.isBlank() ? latex : formula,
                description != null && !description.isBlank() ? description : usage,
                variables
        );
    }

    @JsonProperty(value = "title", access = JsonProperty.Access.READ_ONLY)
    public String getTitle() {
        return name;
    }

    @JsonProperty(value = "formula", access = JsonProperty.Access.READ_ONLY)
    public String getFormula() {
        return latex;
    }

    @JsonProperty(value = "usage", access = JsonProperty.Access.READ_ONLY)
    public String getUsage() {
        return description;
    }
}


