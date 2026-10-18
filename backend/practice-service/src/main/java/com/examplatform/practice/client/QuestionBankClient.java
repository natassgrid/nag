// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.AnswerKeyDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface QuestionBankClient {
    Map<UUID, AnswerKeyDto> getAnswerKeys(List<UUID> questionIds);

    List<UUID> findQuestionIdsBySubject(String subjectPattern, int limit);

    List<UUID> findQuestionsForAdaptiveQuiz(List<String> subtopics, String difficulty, int limit);

    List<UUID> findQuestionsBySubtopics(List<Long> subtopicIds, String difficulty, int limit);
}
