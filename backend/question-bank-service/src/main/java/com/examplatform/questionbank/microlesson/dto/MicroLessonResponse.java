// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty(value = "content", access = JsonProperty.Access.READ_ONLY)
    public MicroLessonStructuredContent getContent() {
        return buildStructuredContent();
    }

    private MicroLessonStructuredContent buildStructuredContent() {
        String safeCore = coreConcept != null ? coreConcept : "";
        String safeTrap = trap != null ? trap : "";

        TrapAlertDto trapAlertDto = new TrapAlertDto(
                "Exam Trap & Common Pitfall",
                !safeTrap.isBlank() ? safeTrap : "Watch out for common exam traps in this subtopic.",
                !safeTrap.isBlank() ? safeTrap : "Applying formulas without checking boundary constraints.",
                !safeCore.isBlank() ? safeCore : "Review foundational principles and standard problem steps."
        );

        java.util.List<FormulaEntryDto> formulas = new java.util.ArrayList<>();
        if (cheatSheet != null) {
            for (int i = 0; i < cheatSheet.size(); i++) {
                String item = cheatSheet.get(i);
                if (item == null || item.isBlank()) continue;
                String name = "Formula " + (i + 1);
                String latex = item.trim();
                String desc = item.trim();
                if (item.contains(":")) {
                    String[] parts = item.split(":", 2);
                    name = parts[0].trim();
                    latex = parts[1].trim();
                    desc = parts[0].trim();
                }
                formulas.add(new FormulaEntryDto(name, latex, desc, java.util.Collections.emptyList()));
            }
        }

        java.util.List<StepItemDto> stepsList = new java.util.ArrayList<>();
        String problemStem = null;
        String finalAns = null;
        if (stepByStepBreakdown != null) {
            problemStem = stepByStepBreakdown.questionStem();
            finalAns = stepByStepBreakdown.finalAnswer();
            java.util.List<String> rawSteps = stepByStepBreakdown.steps();
            if (rawSteps != null) {
                for (int i = 0; i < rawSteps.size(); i++) {
                    String s = rawSteps.get(i);
                    int stepNum = i + 1;
                    String title = "Step " + stepNum;
                    String explanation = s != null ? s : "";
                    String formula = null;
                    if (explanation.contains("$$")) {
                        int firstIdx = explanation.indexOf("$$");
                        int secondIdx = explanation.indexOf("$$", firstIdx + 2);
                        if (secondIdx > firstIdx) {
                            formula = explanation.substring(firstIdx, secondIdx + 2);
                        }
                    }
                    String tip = (i == rawSteps.size() - 1 && finalAns != null) ? ("Final Answer: " + finalAns) : null;
                    stepsList.add(new StepItemDto(stepNum, title, explanation, formula, tip));
                }
            }
        }

        java.util.List<String> takeaways = new java.util.ArrayList<>();
        if (!safeCore.isBlank()) {
            String[] parts = safeCore.split("(?<=[.!?])\\s+");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    takeaways.add(clean);
                }
            }
        }
        if (!safeTrap.isBlank()) {
            takeaways.add("Exam Pitfall: " + safeTrap.trim());
        }
        if (takeaways.isEmpty()) {
            takeaways.add("Master fundamental concepts and practice standard exam questions.");
        }

        java.util.List<QuickCheckItemDto> quickCheck = new java.util.ArrayList<>();
        if (stepByStepBreakdown != null && stepByStepBreakdown.questionStem() != null && !stepByStepBreakdown.questionStem().isBlank()) {
            String stem = stepByStepBreakdown.questionStem();
            String ans = finalAns != null ? finalAns : "Verified standard result";
            quickCheck.add(new QuickCheckItemDto(
                    stem,
                    java.util.List.of(ans, "Incorrect calculation", "Cannot be determined", "Insufficient conditions"),
                    ans,
                    finalAns != null ? ("Final Answer: " + finalAns) : "Follow the step-by-step breakdown above."
            ));
        }

        return new MicroLessonStructuredContent(
                safeCore,
                trapAlertDto,
                formulas,
                stepsList,
                problemStem,
                finalAns,
                takeaways,
                quickCheck
        );
    }
}
