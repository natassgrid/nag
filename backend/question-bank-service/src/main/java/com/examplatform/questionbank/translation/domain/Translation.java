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

package com.examplatform.questionbank.translation.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Translation entity representing a translated version of a question
 * in one of the 22 official Indian (Eighth Schedule) languages.
 *
 * <h3>Storage</h3>
 * <p>The {@code translated_payload} column holds a JSON string
 * ({@link TranslatedQuestionPayload}) serialized by
 * {@link com.examplatform.questionbank.translation.service.TranslationPayloadService}.
 * When {@code payloadEncrypted = true} the JSON is stored as a Vault
 * ciphertext string; when {@code false} it is stored as plain JSON text.
 * The flag is set by the service layer at write time based on the
 * {@code app.translation.encryption.enabled} configuration property
 * (default: {@code false}).
 *
 * <h3>Staleness</h3>
 * <p>{@code sourceVersion} captures {@code question.version} at the time
 * the translation is created.  When the source question is subsequently
 * modified its version increments; the translation service compares stored
 * {@code sourceVersion} against the current question version to detect
 * staleness without a JOIN.
 */
@Entity
@Table(name = "translation", schema = "question_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation extends BaseEntity {

    @Column(name = "question_id", nullable = false, columnDefinition = "uuid")
    private UUID questionId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    /**
     * Structured translation payload serialized as JSON (plain or Vault-encrypted).
     * Use {@link com.examplatform.questionbank.translation.service.TranslationPayloadService}
     * to read and write this field — never access the raw string directly.
     */
    @Column(name = "translated_payload", columnDefinition = "TEXT")
    private String translatedPayload;

    /**
     * {@code true} when {@link #translatedPayload} is Vault-encrypted;
     * {@code false} when it is plain JSON text.
     */
    @Column(name = "payload_encrypted", nullable = false)
    @Builder.Default
    private boolean payloadEncrypted = false;

    /**
     * The {@code question.version} at the time this translation was created.
     * Used to detect staleness: if the source question's current version is
     * greater than this value, the translation should be marked STALE.
     */
    @Column(name = "source_version", nullable = false)
    @Builder.Default
    private long sourceVersion = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranslationStatus status;

    @Column(name = "translator_id", nullable = false, columnDefinition = "uuid")
    private UUID translatorId;

    @Column(name = "reviewer_id", columnDefinition = "uuid")
    private UUID reviewerId;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    /**
     * Translation lifecycle states.
     * <ul>
     *   <li>DRAFT   — submitted by translator, awaiting review</li>
     *   <li>APPROVED — reviewed and approved; safe to serve to candidates</li>
     *   <li>STALE   — source question changed after approval; must be re-translated</li>
     * </ul>
     */
    public enum TranslationStatus {
        DRAFT,
        APPROVED,
        STALE
    }
}
