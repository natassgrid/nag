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

package com.examplatform.questionbank.microlesson.translation.repository;

import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonTranslation;
import com.examplatform.shared.constants.SharedConstants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MicroLessonTranslationRepository extends JpaRepository<MicroLessonTranslation, UUID> {

    List<MicroLessonTranslation> findByMicrolessonIdAndTenantId(UUID microlessonId, String tenantId);

    Optional<MicroLessonTranslation> findByMicrolessonIdAndLanguageCodeAndTenantId(
            UUID microlessonId, String languageCode, String tenantId);

    Optional<MicroLessonTranslation> findByMicrolessonIdAndLanguageCodeAndStatusAndTenantId(
            UUID microlessonId, String languageCode, String status, String tenantId);

    List<MicroLessonTranslation> findByMicrolessonIdAndStatusAndTenantId(
            UUID microlessonId, String status, String tenantId);

    List<MicroLessonTranslation> findByMicrolessonId(UUID microlessonId);

    @Query("""
        SELECT t FROM MicroLessonTranslation t
        WHERE t.microlessonId = :microlessonId
          AND LOWER(t.languageCode) = LOWER(:languageCode)
          AND t.status IN :statuses
          AND (t.tenantId = :tenantId OR t.tenantId = 'default' OR :tenantId IS NULL)
        ORDER BY t.updatedAt DESC
    """)
    List<MicroLessonTranslation> findByMicrolessonIdAndLanguageCodeAndStatusIn(
            @Param("microlessonId") UUID microlessonId,
            @Param("languageCode") String languageCode,
            @Param("statuses") Collection<String> statuses,
            @Param(SharedConstants.TENANT_ID) String tenantId);
}
