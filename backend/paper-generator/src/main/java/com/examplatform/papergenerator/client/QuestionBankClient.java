package com.examplatform.papergenerator.client;

import com.examplatform.papergenerator.dto.QuestionSummary;

import java.util.List;

/**
 * Client interface for inter-service communication with the Question Bank Service.
 * Implementations may use REST, gRPC, or messaging depending on deployment topology.
 *
 * Validates: Requirements 8.1, 8.3
 */
public interface QuestionBankClient {

    /**
     * Finds available questions matching the specified criteria from the question bank.
     *
     * @param subject        the subject to filter by
     * @param topic          the topic to filter by
     * @param difficulty     the difficulty level (EASY/MEDIUM/HARD) or null for any
     * @param cognitiveLevel the cognitive level or null for any
     * @param tenantId       the tenant identifier for multi-tenancy isolation
     * @return list of question summaries matching the criteria
     */
    List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                  String difficulty, String cognitiveLevel, String tenantId);
}
