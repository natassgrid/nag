package com.examplatform.translation.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * in one of the supported Eighth Schedule languages.
 * The translatedContent field is encrypted at rest via Vault Transit.
 */
@Entity
@Table(name = "translation", schema = "translation_service")
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
     * Encrypted content stored as Vault ciphertext (vault:v1:...).
     * Decrypted at read-time via EncryptedFieldConverter.
     */
    @Column(name = "translated_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedFieldConverter.class)
    private String translatedContent;

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
     */
    public enum TranslationStatus {
        DRAFT,
        APPROVED,
        STALE
    }
}
