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
        Instant updatedAt,
        String languageCode,
        Boolean fallbackToEnglish,
        String lessonType,
        String difficultyLevel,
        int sequenceNo,
        UUID prerequisiteLessonId,
        int estimatedMinutes,
        Double qualityScore
) implements Serializable {

    public MicroLessonResponse(
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
            Instant updatedAt,
            String languageCode,
            Boolean fallbackToEnglish
    ) {
        this(id, subjectId, topicId, subtopicId, subject, topic, subtopic, targetExam,
                coreConcept, trap, cheatSheet, stepByStepBreakdown, status, reviewedBy,
                reviewedAt, reviewComments, contentVersion, createdAt, updatedAt,
                languageCode, fallbackToEnglish, "CONCEPT", "CORE", 1, null, 5, null);
    }

    public MicroLessonResponse(
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
    ) {
        this(id, subjectId, topicId, subtopicId, subject, topic, subtopic, targetExam,
                coreConcept, trap, cheatSheet, stepByStepBreakdown, status, reviewedBy,
                reviewedAt, reviewComments, contentVersion, createdAt, updatedAt, "en", false,
                "CONCEPT", "CORE", 1, null, 5, null);
    }

    public MicroLessonResponse withLocalization(String languageCode, boolean fallbackToEnglish) {
        return new MicroLessonResponse(
                id, subjectId, topicId, subtopicId, subject, topic, subtopic, targetExam,
                coreConcept, trap, cheatSheet, stepByStepBreakdown, status, reviewedBy,
                reviewedAt, reviewComments, contentVersion, createdAt, updatedAt,
                languageCode, fallbackToEnglish, lessonType, difficultyLevel, sequenceNo,
                prerequisiteLessonId, estimatedMinutes, qualityScore
        );
    }

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

    @JsonProperty(value = "approachName", access = JsonProperty.Access.READ_ONLY)
    public String getApproachName() {
        if (lessonType == null || lessonType.isBlank()) {
            return "Core Micro-Tutorial";
        }
        return switch (lessonType.trim().toUpperCase()) {
            case "CONCEPT" -> "Conceptual Foundation";
            case "WORKED_EXAMPLE", "WORKED_EXAMPLES" -> "Step-by-Step Worked Method";
            case "TRAPS_AND_PITFALLS" -> "Exam Traps & Elimination";
            case "CHEAT_SHEET" -> "Formula & Shortcut Sheet";
            case "VISUAL" -> "Visual & Intuitive Model";
            case "QUICK_RECAP" -> "Rapid Revision Recap";
            default -> "Core Micro-Tutorial";
        };
    }

    @JsonProperty(value = "title", access = JsonProperty.Access.READ_ONLY)
    public String getTitle() {
        int seq = sequenceNo > 0 ? sequenceNo : 1;
        return "Approach " + seq + ": " + getApproachName();
    }

    @JsonProperty(value = "formulaCheatSheet", access = JsonProperty.Access.READ_ONLY)
    public List<FormulaEntryDto> getFormulaCheatSheet() {
        return buildStructuredContent().formulaCheatSheet();
    }

    @JsonProperty(value = "keyTakeaways", access = JsonProperty.Access.READ_ONLY)
    public List<String> getKeyTakeaways() {
        return buildStructuredContent().keyTakeaways();
    }

    @JsonProperty(value = "quickCheckItems", access = JsonProperty.Access.READ_ONLY)
    public List<QuickCheckItemDto> getQuickCheckItems() {
        return buildStructuredContent().quickCheckItems();
    }

    @JsonProperty(value = "content", access = JsonProperty.Access.READ_ONLY)
    public MicroLessonStructuredContent getContent() {
        return buildStructuredContent();
    }

    private MicroLessonStructuredContent buildStructuredContent() {
        String safeCore = coreConcept != null ? coreConcept : "";
        String safeTrap = trap != null ? trap : "";

        TrapAlertDto trapAlertDto;
        if (stepByStepBreakdown != null && stepByStepBreakdown.structuredTrapAlert() != null) {
            TrapAlertDto authored = stepByStepBreakdown.structuredTrapAlert();
            trapAlertDto = new TrapAlertDto(
                    authored.title() != null && !authored.title().isBlank() ? authored.title() : "Exam Trap & Common Pitfall",
                    authored.description() != null && !authored.description().isBlank() ? authored.description() : safeTrap,
                    authored.commonMistake() != null ? authored.commonMistake() : safeTrap,
                    authored.correctApproach() != null ? authored.correctApproach() : safeCore
            );
        } else {
            trapAlertDto = new TrapAlertDto(
                    "Exam Trap & Common Pitfall",
                    !safeTrap.isBlank() ? safeTrap : "Watch out for common exam traps in this subtopic.",
                    !safeTrap.isBlank() ? safeTrap : "Applying formulas without checking boundary constraints.",
                    !safeCore.isBlank() ? safeCore : "Review foundational principles and standard problem steps."
            );
        }

        java.util.List<FormulaEntryDto> formulas = new java.util.ArrayList<>();
        if (stepByStepBreakdown != null
                && stepByStepBreakdown.structuredFormulas() != null
                && !stepByStepBreakdown.structuredFormulas().isEmpty()) {
            formulas.addAll(stepByStepBreakdown.structuredFormulas());
        } else if (cheatSheet != null) {
            for (int i = 0; i < cheatSheet.size(); i++) {
                String item = cheatSheet.get(i);
                if (item == null || item.isBlank()) continue;
                String trimmed = item.trim();
                int colonIdx = trimmed.indexOf(':');
                int firstDollarIdx = trimmed.indexOf('$');
                int firstSlashIdx = trimmed.indexOf('\\');

                boolean hasColon = colonIdx > 0;
                boolean colonBeforeDollar = firstDollarIdx == -1 || colonIdx < firstDollarIdx;
                boolean colonBeforeSlash = firstSlashIdx == -1 || colonIdx < firstSlashIdx;

                String name = "Formula " + (i + 1);
                String latex = trimmed;
                String desc = trimmed;

                if (hasColon && colonBeforeDollar && colonBeforeSlash) {
                    String namePart = trimmed.substring(0, colonIdx).trim();
                    String latexPart = trimmed.substring(colonIdx + 1).trim();
                    if (!namePart.isEmpty() && !namePart.contains("$") && !namePart.contains("\\")) {
                        name = namePart;
                        latex = latexPart;
                        desc = namePart;
                    }
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
            if (stepByStepBreakdown.structuredSteps() != null && !stepByStepBreakdown.structuredSteps().isEmpty()) {
                stepsList.addAll(stepByStepBreakdown.structuredSteps());
            } else {
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
        }

        java.util.List<String> takeaways = new java.util.ArrayList<>();
        if (stepByStepBreakdown != null
                && stepByStepBreakdown.keyTakeaways() != null
                && !stepByStepBreakdown.keyTakeaways().isEmpty()) {
            takeaways.addAll(stepByStepBreakdown.keyTakeaways());
        } else {
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
        }

        java.util.List<QuickCheckItemDto> quickCheck = new java.util.ArrayList<>();
        if (stepByStepBreakdown != null
                && stepByStepBreakdown.quickCheckItems() != null
                && !stepByStepBreakdown.quickCheckItems().isEmpty()) {
            quickCheck.addAll(stepByStepBreakdown.quickCheckItems());
        } else if (stepByStepBreakdown != null
                && stepByStepBreakdown.questionStem() != null
                && !stepByStepBreakdown.questionStem().isBlank()) {
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
