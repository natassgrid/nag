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

package com.examplatform.questionbank.domain;

import com.examplatform.questionbank.crypto.EncryptedFieldConverter;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Stimulus/Passage entity shared across 2–6 comprehension or case study sub-questions.
 * Stores versioned passage text encrypted at rest via {@link EncryptedFieldConverter}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "passage", schema = "question_service")
public class Passage extends BaseEntity {

    @Column(name = "title", length = 500)
    private String title;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = SharedConstants.CONTENT, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_format", nullable = false, length = 20)
    @Builder.Default
    private String contentFormat = "MIXED";

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "subtopic_id")
    private Long subtopicId;

    @Column(name = SharedConstants.SUBJECT, nullable = false, length = 100)
    private String subject;

    @Column(name = "topic", length = 200)
    private String topic;

    @Column(name = "has_images", nullable = false)
    @Builder.Default
    private boolean hasImages = false;

    @Column(name = "state", nullable = false, length = 20)
    @Builder.Default
    private String state = QuestionStateConstants.DRAFT;

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Transient
    private float[] embedding;
}
