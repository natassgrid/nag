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

package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByIdAndTenantId(Long id, String tenantId);

    Optional<Topic> findByIdAndTenantIdIsNull(Long id);

    Optional<Topic> findByIdAndSubjectIdAndTenantId(Long id, Long subjectId, String tenantId);

    Optional<Topic> findByIdAndSubjectIdAndTenantIdIsNull(Long id, Long subjectId);

    List<Topic> findBySubjectIdAndTenantId(Long subjectId, String tenantId);

    List<Topic> findBySubjectIdAndTenantIdIsNull(Long subjectId);

    List<Topic> findBySubjectIdAndTenantIdOrderByNameAsc(Long subjectId, String tenantId);

    List<Topic> findBySubjectIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(Long subjectId);

    List<Topic> findBySubjectIdAndTenantIdOrderByDisplayOrderAscNameAsc(Long subjectId, String tenantId);

    List<Topic> findBySubjectIdInAndTenantIdIsNull(Collection<Long> subjectIds);

    List<Topic> findBySubjectIdInAndTenantId(Collection<Long> subjectIds, String tenantId);

    Optional<Topic> findByNameAndSubjectIdAndTenantId(String name, Long subjectId, String tenantId);

    Optional<Topic> findByNameAndSubjectIdAndTenantIdIsNull(String name, Long subjectId);

    Optional<Topic> findBySubjectIdAndCodeAndTenantId(Long subjectId, String code, String tenantId);

    Optional<Topic> findBySubjectIdAndCodeAndTenantIdIsNull(Long subjectId, String code);

    boolean existsByNameAndSubjectIdAndTenantId(String name, Long subjectId, String tenantId);

    boolean existsByNameAndSubjectIdAndTenantIdIsNull(String name, Long subjectId);

    boolean existsByCodeAndSubjectIdAndTenantId(String code, Long subjectId, String tenantId);

    boolean existsByCodeAndSubjectIdAndTenantIdIsNull(String code, Long subjectId);
}
