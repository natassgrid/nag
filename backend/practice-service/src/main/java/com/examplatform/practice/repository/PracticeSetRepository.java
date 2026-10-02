// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.repository;
import com.examplatform.practice.domain.PracticeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface PracticeSetRepository extends JpaRepository<PracticeSet, UUID> {
    List<PracticeSet> findByPublishedTrue();
}
