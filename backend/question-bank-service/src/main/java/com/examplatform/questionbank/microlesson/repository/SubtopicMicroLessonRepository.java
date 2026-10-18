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

package com.examplatform.questionbank.microlesson.repository;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubtopicMicroLessonRepository extends JpaRepository<SubtopicMicroLesson, UUID>, JpaSpecificationExecutor<SubtopicMicroLesson> {

    List<SubtopicMicroLesson> findBySubtopicIdOrderBySequenceNoAsc(Long subtopicId);

    List<SubtopicMicroLesson> findBySubtopicIdAndStatusOrderBySequenceNoAsc(Long subtopicId, String status);

    Optional<SubtopicMicroLesson> findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(Long subtopicId, String status);

    Optional<SubtopicMicroLesson> findTopBySubtopicIdOrderByContentVersionDesc(Long subtopicId);

    Optional<SubtopicMicroLesson> findBySubtopicIdAndLessonTypeAndDifficultyLevelAndSequenceNo(
            Long subtopicId, String lessonType, String difficultyLevel, int sequenceNo);

    Page<SubtopicMicroLesson> findByStatus(String status, Pageable pageable);

    Page<SubtopicMicroLesson> findByStatusAndSubjectId(String status, Long subjectId, Pageable pageable);

    Page<SubtopicMicroLesson> findBySubjectId(Long subjectId, Pageable pageable);

    boolean existsBySubtopicIdAndStatusIn(Long subtopicId, Collection<String> statuses);

    long countByStatus(String status);
}
