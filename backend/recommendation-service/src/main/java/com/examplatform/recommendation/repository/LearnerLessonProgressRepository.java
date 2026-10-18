/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.repository;

import com.examplatform.recommendation.domain.LearnerLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerLessonProgressRepository extends JpaRepository<LearnerLessonProgress, UUID> {
    Optional<LearnerLessonProgress> findByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);
    List<LearnerLessonProgress> findByEnrollmentId(UUID enrollmentId);
    List<LearnerLessonProgress> findByEnrollmentIdAndSubtopicId(UUID enrollmentId, Long subtopicId);
    boolean existsByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);
}
