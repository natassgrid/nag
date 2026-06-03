package com.examplatform.examination.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Section POJO embedded within the Examination JSONB column.
 * NOT a JPA entity — serialized/deserialized as part of sectionsJson.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Section {
    private String name;
    private String subject;
    private int questionCount;
    private double marksPerQuestion;
    private double negativeMarksPerQuestion;
    private List<SubjectTopicRule> topicRules;
}
