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

import com.examplatform.questionbank.domain.Subtopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubtopicRepository extends JpaRepository<Subtopic, Long> {

    Optional<Subtopic> findByIdAndTenantId(Long id, String tenantId);

    Optional<Subtopic> findByIdAndTenantIdIsNull(Long id);

    Optional<Subtopic> findByIdAndTopicIdAndTenantId(Long id, Long topicId, String tenantId);

    Optional<Subtopic> findByIdAndTopicIdAndTenantIdIsNull(Long id, Long topicId);

    List<Subtopic> findByTopicIdAndTenantId(Long topicId, String tenantId);

    List<Subtopic> findByTopicIdAndTenantIdIsNull(Long topicId);

    List<Subtopic> findByTopicIdAndTenantIdOrderByNameAsc(Long topicId, String tenantId);

    List<Subtopic> findByTopicIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(Long topicId);

    List<Subtopic> findByTopicIdAndTenantIdOrderByDisplayOrderAscNameAsc(Long topicId, String tenantId);

    List<Subtopic> findByTopicIdInAndTenantIdIsNull(Collection<Long> topicIds);

    List<Subtopic> findByTopicIdInAndTenantId(Collection<Long> topicIds, String tenantId);

    Optional<Subtopic> findByNameAndTopicIdAndTenantId(String name, Long topicId, String tenantId);

    Optional<Subtopic> findByNameAndTopicIdAndTenantIdIsNull(String name, Long topicId);

    Optional<Subtopic> findByTopicIdAndCodeAndTenantId(Long topicId, String code, String tenantId);

    Optional<Subtopic> findByTopicIdAndCodeAndTenantIdIsNull(Long topicId, String code);

    boolean existsByNameAndTopicIdAndTenantId(String name, Long topicId, String tenantId);

    boolean existsByNameAndTopicIdAndTenantIdIsNull(String name, Long topicId);

    boolean existsByCodeAndTopicIdAndTenantId(String code, Long topicId, String tenantId);

    boolean existsByCodeAndTopicIdAndTenantIdIsNull(String code, Long topicId);
}
