package com.examplatform.questionbank.domain.enums;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum QuestionType {

    /** Single-correct MCQ — legacy alias "MCQ" accepted from older clients */
    @JsonAlias("MCQ")
    SINGLE_MCQ,

    /** Multiple-correct MCQ — legacy alias "MSQ" accepted from older clients */
    @JsonAlias("MSQ")
    MULTI_MCQ,

    NUMERICAL,
    DESCRIPTIVE,
    MATRIX_MATCH,
    ASSERTION_REASON,
    CODING,
    CASE_STUDY
}
