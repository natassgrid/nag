/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\n *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.translation.repository;

import com.examplatform.questionbank.translation.domain.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for translations.
 */
@Repository
public interface TranslationRepository extends JpaRepository<Translation, UUID> {

    List<Translation> findByQuestionIdAndTenantId(UUID questionId, String tenantId);

    List<Translation> findByQuestionIdAndLanguageCodeAndTenantId(UUID questionId, String languageCode, String tenantId);

    List<Translation> findByQuestionIdAndStatusAndTenantId(UUID questionId, Translation.TranslationStatus status, String tenantId);

    List<Translation> findByTranslatorIdAndTenantId(UUID translatorId, String tenantId);

    List<Translation> findByStatusAndTenantId(Translation.TranslationStatus status, String tenantId);
}
