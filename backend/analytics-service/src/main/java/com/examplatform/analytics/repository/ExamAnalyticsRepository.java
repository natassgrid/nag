package com.examplatform.analytics.repository;

import com.examplatform.analytics.domain.ExamAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAnalyticsRepository extends JpaRepository<ExamAnalytics, UUID> {

    Optional<ExamAnalytics> findTopByExamIdOrderByComputedAtDesc(UUID examId);
}
