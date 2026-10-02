// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.repository;
import com.examplatform.practice.domain.PracticeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface PracticeResponseRepository extends JpaRepository<PracticeResponse, UUID> {
    List<PracticeResponse> findByPracticeSessionIdOrderByRevisionSequenceDesc(UUID practiceSessionId);
    List<PracticeResponse> findByPracticeSessionId(UUID practiceSessionId);
}
