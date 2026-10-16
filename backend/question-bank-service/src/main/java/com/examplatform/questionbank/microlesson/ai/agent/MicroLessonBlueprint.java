// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.domain.Question;

import java.util.List;

/**
 * Structured pedagogical blueprint containing domain classification,
 * tailored pedagogical directives, and real sample questions from QuestionRepository.
 */
public record MicroLessonBlueprint(
        DomainCategory domainCategory,
        String subject,
        String topic,
        String subtopic,
        String targetExam,
        String difficulty,
        String domainDirective,
        List<Question> sampleQuestions,
        String formattedBlueprintPrompt
) {}
