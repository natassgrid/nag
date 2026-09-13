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

package com.examplatform.questionbank.translation.repository;

import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchTranslationJobRepository extends JpaRepository<BatchTranslationJob, UUID> {

    Optional<BatchTranslationJob> findByIdAndTenantId(UUID id, String tenantId);

    List<BatchTranslationJob> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<BatchTranslationJob> findByStatusAndTenantId(BatchTranslationJobStatus status, String tenantId);

    @Modifying
    @Transactional
    @Query("UPDATE BatchTranslationJob j SET j.processedQuestions = j.processedQuestions + 1, j.successfulQuestions = j.successfulQuestions + 1 WHERE j.id = :jobId")
    int incrementSuccess(@Param("jobId") UUID jobId);

    @Modifying
    @Transactional
    @Query("UPDATE BatchTranslationJob j SET j.processedQuestions = j.processedQuestions + 1, j.failedQuestions = j.failedQuestions + 1 WHERE j.id = :jobId")
    int incrementFailure(@Param("jobId") UUID jobId);
}
