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

import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
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
 * Spring Data JPA repository for {@link QuestionPaperMetadata}.
 */
@Repository
public interface QuestionPaperMetadataRepository
        extends JpaRepository<QuestionPaperMetadata, UUID>, JpaSpecificationExecutor<QuestionPaperMetadata> {

    Optional<QuestionPaperMetadata> findByIdAndTenantId(UUID id, String tenantId);

    Optional<QuestionPaperMetadata> findFirstByTenantIdAndFileSha256OrderByCreatedAtDesc(String tenantId, String fileSha256);

    Page<QuestionPaperMetadata> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<QuestionPaperMetadata> findByExtractionStatusInOrderByCreatedAtAsc(Collection<PaperExtractionStatus> statuses);
}
