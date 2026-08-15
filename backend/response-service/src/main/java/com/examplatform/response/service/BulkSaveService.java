/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.response.service;

import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.BulkSaveRequest;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for bulk-saving offline-buffered responses with deduplication.
 * Guarantees zero data loss: every unique response is persisted exactly once.
 *
 * For each response in the ordered list:
 * - If buffered response's revisionSequence > server-side max → save it (new data)
 * - If buffered response's revisionSequence <= server-side max → skip (already persisted)
 *
 * Validates: Requirements 10.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BulkSaveService {

    private final ResponseRepository responseRepository;

    /**
     * Reconciles and persists offline-buffered responses.
     *
     * @param sessionId   the exam session ID
     * @param request     the bulk save request containing ordered responses
     * @param candidateId the candidate's user ID
     * @param tenantId    the tenant identifier
     * @return list of save confirmations (only for newly persisted responses)
     */
    public List<SaveResponseResponse> bulkSave(UUID sessionId, BulkSaveRequest request,
                                                UUID candidateId, String tenantId) {
        List<SaveResponseResponse> results = new ArrayList<>();

        for (SaveResponseRequest saveRequest : request.getResponses()) {
            // Look up current max revisionSequence for (sessionId, questionId)
            List<Response> existing = responseRepository
                    .findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, saveRequest.getQuestionId());

            int serverMaxRevision = existing.isEmpty() ? 0 : existing.get(0).getRevisionSequence();
            int bufferedRevision = saveRequest.getRevisionSequence() != null
                    ? saveRequest.getRevisionSequence()
                    : serverMaxRevision + 1;

            if (bufferedRevision > serverMaxRevision) {
                // New data — save it
                Response response = Response.builder()
                        .sessionId(sessionId)
                        .questionId(saveRequest.getQuestionId())
                        .candidateId(candidateId)
                        .selectedOptionIds(saveRequest.getSelectedOptionIds())
                        .enteredValue(saveRequest.getEnteredValue())
                        .timestamp(saveRequest.getTimestamp())
                        .cumulativeTimeSpentMs(saveRequest.getCumulativeTimeSpentMs())
                        .revisionSequence(bufferedRevision)
                        .saveSource(saveRequest.getSaveSource())
                        .isFinal(false)
                        .build();
                response.setTenantId(tenantId);

                Response saved = responseRepository.save(response);

                results.add(SaveResponseResponse.builder()
                        .responseId(saved.getId())
                        .sessionId(sessionId)
                        .questionId(saveRequest.getQuestionId())
                        .revisionSequence(bufferedRevision)
                        .saveSource(saveRequest.getSaveSource())
                        .savedAt(saved.getCreatedAt())
                        .build());

                log.debug("Bulk-save persisted: sessionId={}, questionId={}, revision={}",
                        sessionId, saveRequest.getQuestionId(), bufferedRevision);
            } else {
                // Already persisted — skip
                log.debug("Bulk-save skipped (already persisted): sessionId={}, questionId={}, bufferedRevision={}, serverMax={}",
                        sessionId, saveRequest.getQuestionId(), bufferedRevision, serverMaxRevision);
            }
        }

        log.info("Bulk-save completed: sessionId={}, candidate={}, total={}, persisted={}, skipped={}",
                sessionId, candidateId, request.getResponses().size(), results.size(),
                request.getResponses().size() - results.size());

        return results;
    }
}
