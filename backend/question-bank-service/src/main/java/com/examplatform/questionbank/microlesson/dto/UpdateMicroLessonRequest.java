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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateMicroLessonRequest implements Serializable {

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
        this.subjectId = CreateMicroLessonRequest.parseFlexibleLong(raw);
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    @JsonSetter("topicId")
    public void setTopicIdRaw(Object raw) {
        this.topicId = CreateMicroLessonRequest.parseFlexibleLong(raw);
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    @JsonSetter("subtopicId")
    public void setSubtopicIdRaw(Object raw) {
        this.subtopicId = CreateMicroLessonRequest.parseFlexibleLong(raw);
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
        this.formulaCheatSheet = CreateMicroLessonRequest.parseFlexibleFormulas(raw);
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
}
