// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.repository;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubtopicMicroLessonRepository extends JpaRepository<SubtopicMicroLesson, UUID>, JpaSpecificationExecutor<SubtopicMicroLesson> {

    Optional<SubtopicMicroLesson> findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(Long subtopicId, String status);

    Optional<SubtopicMicroLesson> findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
            Long subtopicId, String targetExam, String status);

    Optional<SubtopicMicroLesson> findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(
            Long subtopicId, String targetExam);

    Optional<SubtopicMicroLesson> findTopBySubtopicIdOrderByContentVersionDesc(Long subtopicId);

    Page<SubtopicMicroLesson> findByStatus(String status, Pageable pageable);

    Page<SubtopicMicroLesson> findByStatusAndSubjectId(String status, Long subjectId, Pageable pageable);

    Page<SubtopicMicroLesson> findBySubjectId(Long subjectId, Pageable pageable);

    boolean existsBySubtopicIdAndTargetExamAndStatusIn(Long subtopicId, String targetExam, Collection<String> statuses);

    long countByStatus(String status);

    @Query("SELECT DISTINCT l.subject FROM SubtopicMicroLesson l WHERE l.subject IS NOT NULL AND l.subject <> '' ORDER BY l.subject ASC")
    List<String> findDistinctSubjects();

    @Query("SELECT DISTINCT l.topic FROM SubtopicMicroLesson l WHERE l.topic IS NOT NULL AND l.topic <> '' AND (:subject IS NULL OR :subject = '' OR :subject = 'ALL' OR LOWER(l.subject) = LOWER(:subject)) ORDER BY l.topic ASC")
    List<String> findDistinctTopics(@Param("subject") String subject);
}
