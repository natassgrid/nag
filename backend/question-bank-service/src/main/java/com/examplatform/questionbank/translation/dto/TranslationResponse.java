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

package com.examplatform.questionbank.translation.dto;

import com.examplatform.questionbank.translation.domain.Translation.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only response DTO for a translation, used both in admin views and
 * by the delivery service to serve localized content to candidates.
 *
 * <p>The translated payload fields ({@link #translatedContent},
 * {@link #translatedOptions}, {@link #translatedExplanation}) are already
 * deserialized (and decrypted if necessary) before this DTO is returned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {

    private UUID translationId;
    private UUID questionId;
    private String languageCode;

    /** Translated question body. */
    private String translatedContent;

    /** Translated options (id + text). */
    private List<TranslatedOptionDto> translatedOptions;

    /** Translated explanation (may be null). */
    private String translatedExplanation;

    /** Question version at the time this translation was created. */
    private long sourceVersion;

    private TranslationStatus status;
    private UUID translatorId;
    private UUID reviewerId;
    private String reviewComments;

    private Instant createdAt;
    private Instant updatedAt;
}
