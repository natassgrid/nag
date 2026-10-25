/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.repository;

import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ExtractedQuestion}.
 */
@Repository
public interface ExtractedQuestionRepository
        extends JpaRepository<ExtractedQuestion, UUID>, JpaSpecificationExecutor<ExtractedQuestion> {

    Page<ExtractedQuestion> findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(
            UUID paperMetadataId, String tenantId, Pageable pageable);

    List<ExtractedQuestion> findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(
            UUID paperMetadataId, String tenantId);

    Optional<ExtractedQuestion> findByPaperMetadataIdAndQuestionNumber(
            UUID paperMetadataId, Integer questionNumber);

    Optional<ExtractedQuestion> findByIdAndPaperMetadataIdAndTenantId(
            UUID id, UUID paperMetadataId, String tenantId);

    List<ExtractedQuestion> findByPaperMetadataIdAndStatusAndTenantIdOrderByQuestionNumberAsc(
            UUID paperMetadataId, ExtractedQuestionStatus status, String tenantId);

    List<ExtractedQuestion> findByIdInAndPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(
            Collection<UUID> ids, UUID paperMetadataId, String tenantId);

    long countByPaperMetadataIdAndTenantId(UUID paperMetadataId, String tenantId);

    long countByPaperMetadataIdAndStatusAndTenantId(
            UUID paperMetadataId, ExtractedQuestionStatus status, String tenantId);

    void deleteByPaperMetadataIdAndTenantId(UUID paperMetadataId, String tenantId);

    void deleteByIdInAndPaperMetadataIdAndTenantId(Collection<UUID> ids, UUID paperMetadataId, String tenantId);

    void deleteByPaperMetadataIdAndSourcePageNumber(UUID paperMetadataId, Integer sourcePageNumber);
}
