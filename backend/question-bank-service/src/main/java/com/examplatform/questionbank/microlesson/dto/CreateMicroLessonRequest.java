// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateMicroLessonRequest implements Serializable {

    private Long subjectId;

    @JsonAlias({"subjectName"})
    private String subject;

    private Long topicId;

    @JsonAlias({"topicName"})
    private String topic;

    private Long subtopicId;

    @JsonAlias({"subtopicName"})
    private String subtopic;

    private String targetExam;

    private String lessonType;
    private String difficultyLevel;
    private Integer sequenceNo;
    private UUID prerequisiteLessonId;
    private Integer estimatedMinutes;
    private String language;
    private Double qualityScore;

    private String coreConcept;

    private String trap;

    private TrapAlertDto trapAlert;

    private List<FormulaEntryDto> formulaCheatSheet;

    private List<String> cheatSheet;

    private StepByStepBreakdownDto stepByStepBreakdown;

    @JsonAlias({"questionStem"})
    private String problemStatement;

    private String finalAnswer;

    private List<String> keyTakeaways;

    @JsonAlias({"quickChecks"})
    private List<QuickCheckItemDto> quickCheckItems;

    private String status;

    @JsonAlias({"reviewerNotes", "reviewNotes", "comments"})
    private String reviewComments;

    private MicroLessonStructuredContent content;

    @JsonSetter("subjectId")
    public void setSubjectIdRaw(Object raw) {
        this.subjectId = parseFlexibleLong(raw);
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    @JsonSetter("topicId")
    public void setTopicIdRaw(Object raw) {
        this.topicId = parseFlexibleLong(raw);
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    @JsonSetter("subtopicId")
    public void setSubtopicIdRaw(Object raw) {
        this.subtopicId = parseFlexibleLong(raw);
    }

    public void setSubtopicId(Long subtopicId) {
        this.subtopicId = subtopicId;
    }

    @JsonSetter("trapAlert")
    public void setTrapAlertRaw(Object raw) {
        this.trapAlert = StepByStepBreakdownDto.toTrapAlertDto(raw);
    }

    public void setTrapAlert(TrapAlertDto trapAlert) {
        this.trapAlert = trapAlert;
    }

    @JsonSetter("formulaCheatSheet")
    public void setFormulaCheatSheetRaw(Object raw) {
        this.formulaCheatSheet = parseFlexibleFormulas(raw);
    }

    public void setFormulaCheatSheet(List<FormulaEntryDto> formulaCheatSheet) {
        this.formulaCheatSheet = formulaCheatSheet;
    }

    // Record-style fluent accessors for seamless interoperability
    public Long subjectId() { return subjectId; }
    public String subject() { return subject; }
    public Long topicId() { return topicId; }
    public String topic() { return topic; }
    public Long subtopicId() { return subtopicId; }
    public String subtopic() { return subtopic; }
    public String targetExam() { return targetExam; }
    public String lessonType() { return lessonType; }
    public String difficultyLevel() { return difficultyLevel; }
    public Integer sequenceNo() { return sequenceNo; }
    public UUID prerequisiteLessonId() { return prerequisiteLessonId; }
    public Integer estimatedMinutes() { return estimatedMinutes; }
    public String language() { return language; }
    public Double qualityScore() { return qualityScore; }
    public String coreConcept() { return coreConcept; }
    public String trap() { return trap; }
    public TrapAlertDto trapAlert() { return trapAlert; }
    public List<FormulaEntryDto> formulaCheatSheet() { return formulaCheatSheet; }
    public List<String> cheatSheet() { return cheatSheet; }
    public StepByStepBreakdownDto stepByStepBreakdown() { return stepByStepBreakdown; }
    public String problemStatement() { return problemStatement; }
    public String finalAnswer() { return finalAnswer; }
    public List<String> keyTakeaways() { return keyTakeaways; }
    public List<QuickCheckItemDto> quickCheckItems() { return quickCheckItems; }
    public String status() { return status; }
    public String reviewComments() { return reviewComments; }
    public MicroLessonStructuredContent content() { return content; }

    static Long parseFlexibleLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s) {
            return parseStringToLong(s);
        }
        return null;
    }

    private static Long parseStringToLong(String s) {
        String text = s.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException _) {
            return fallbackHash(text);
        }
    }

    private static long fallbackHash(String text) {
        try {
            UUID uuid = UUID.fromString(text);
            long hash = (uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits()) & Long.MAX_VALUE;
            return hash > 0 ? hash : 1L;
        } catch (IllegalArgumentException _) {
            long hash = text.hashCode() & 0x7FFFFFFFL;
            return hash > 0 ? hash : 1L;
        }
    }

    static List<FormulaEntryDto> parseFlexibleFormulas(Object raw) {
        if (raw instanceof String s) {
            return parseFormulasFromString(s);
        }
        if (raw instanceof List<?> list) {
            return parseFormulasFromList(list);
        }
        return Collections.emptyList();
    }

    private static List<FormulaEntryDto> parseFormulasFromString(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        List<FormulaEntryDto> result = new ArrayList<>();
        String[] parts = trimmed.split("\\n\\n+");
        for (int i = 0; i < parts.length; i++) {
            FormulaEntryDto dto = StepByStepBreakdownDto.toFormulaEntryDto(parts[i], i + 1);
            if (dto != null) {
                result.add(dto);
            }
        }
        return result;
    }

    private static List<FormulaEntryDto> parseFormulasFromList(List<?> list) {
        List<FormulaEntryDto> result = new ArrayList<>();
        int idx = 1;
        for (Object item : list) {
            FormulaEntryDto dto = StepByStepBreakdownDto.toFormulaEntryDto(item, idx++);
            if (dto != null) {
                result.add(dto);
            }
        }
        return result;
    }
}
