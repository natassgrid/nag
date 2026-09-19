/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\
 */

package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Passage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PassageRepository extends JpaRepository<Passage, UUID>, JpaSpecificationExecutor<Passage> {

    Optional<Passage> findByIdAndTenantId(UUID id, String tenantId);

    Page<Passage> findByTenantId(String tenantId, Pageable pageable);

    Page<Passage> findByTenantIdAndState(String tenantId, String state, Pageable pageable);

    Page<Passage> findByTenantIdAndSubjectId(String tenantId, Long subjectId, Pageable pageable);

    List<Passage> findByTenantIdAndState(String tenantId, String state);

    List<Passage> findByIdInAndTenantId(List<UUID> ids, String tenantId);

    List<Passage> findByAuthorIdAndTenantId(UUID authorId, String tenantId);
}
