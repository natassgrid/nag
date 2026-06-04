package com.examplatform.papergenerator.client;

import com.examplatform.papergenerator.dto.QuestionSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of QuestionBankClient for local development.
 * In production, this would make REST calls to the question-bank-service.
 */
@Slf4j
@Component
public class QuestionBankClientImpl implements QuestionBankClient {

    @Override
    public List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                        String difficulty, String cognitiveLevel, String tenantId) {
        log.info("[STUB] Finding questions: subject={}, topic={}, difficulty={}, tenant={}",
                subject, topic, difficulty, tenantId);
        return Collections.emptyList();
    }
}
