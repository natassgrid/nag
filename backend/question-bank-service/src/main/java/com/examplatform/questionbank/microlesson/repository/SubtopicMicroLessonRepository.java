// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.repository;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubtopicMicroLessonRepository extends JpaRepository<SubtopicMicroLesson, UUID> {

    Optional<SubtopicMicroLesson> findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(Long subtopicId, String status);

    Optional<SubtopicMicroLesson> findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
            Long subtopicId, String targetExam, String status);

    Optional<SubtopicMicroLesson> findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(
            Long subtopicId, String targetExam);

    Page<SubtopicMicroLesson> findByStatus(String status, Pageable pageable);

    Page<SubtopicMicroLesson> findByStatusAndSubjectId(String status, Long subjectId, Pageable pageable);

    Page<SubtopicMicroLesson> findBySubjectId(Long subjectId, Pageable pageable);

    boolean existsBySubtopicIdAndTargetExamAndStatusIn(Long subtopicId, String targetExam, Collection<String> statuses);
}
