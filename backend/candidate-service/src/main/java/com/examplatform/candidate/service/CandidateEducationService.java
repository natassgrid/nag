/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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

package com.examplatform.candidate.service;

import com.examplatform.candidate.domain.CandidateEducation;
import com.examplatform.candidate.dto.CandidateEducationRequest;
import com.examplatform.candidate.dto.CandidateEducationResponse;
import com.examplatform.candidate.exception.EducationNotFoundException;
import com.examplatform.candidate.repository.CandidateEducationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing candidate educational details and academic qualification history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CandidateEducationService {

    private final CandidateEducationRepository candidateEducationRepository;

    /**
     * Retrieves all educational records for a candidate, ordered by passing year.
     */
    @Transactional(readOnly = true)
    public List<CandidateEducationResponse> getEducationByUserId(UUID userId, String tenantId) {
        return candidateEducationRepository.findByUserIdAndTenantIdOrderByPassingYearAsc(userId, tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new educational qualification record for a candidate.
     */
    public CandidateEducationResponse addEducation(UUID userId, CandidateEducationRequest request, String tenantId) {
        CandidateEducation education = new CandidateEducation();
        BeanUtils.copyProperties(request, education);
        education.setUserId(userId);
        education.setTenantId(tenantId);

        CandidateEducation saved = candidateEducationRepository.save(education);
        log.info("Added education record id={} for candidate userId={} in tenant={}", saved.getId(), userId, tenantId);
        return toResponse(saved);
    }

    /**
     * Updates an existing educational qualification record for a candidate.
     */
    public CandidateEducationResponse updateEducation(
            UUID userId,
            UUID educationId,
            CandidateEducationRequest request,
            String tenantId) {
        CandidateEducation education = candidateEducationRepository
                .findByIdAndUserIdAndTenantId(educationId, userId, tenantId)
                .orElseThrow(() -> new EducationNotFoundException(
                        "Educational record not found for id=" + educationId + " and userId=" + userId));

        BeanUtils.copyProperties(request, education, "id", "userId", "tenantId", "createdAt", "updatedAt");

        CandidateEducation updated = candidateEducationRepository.save(education);
        log.info("Updated education record id={} for candidate userId={} in tenant={}", educationId, userId, tenantId);
        return toResponse(updated);
    }

    /**
     * Deletes a specific educational record.
     */
    public void deleteEducation(UUID userId, UUID educationId, String tenantId) {
        CandidateEducation education = candidateEducationRepository
                .findByIdAndUserIdAndTenantId(educationId, userId, tenantId)
                .orElseThrow(() -> new EducationNotFoundException(
                        "Educational record not found for id=" + educationId + " and userId=" + userId));

        candidateEducationRepository.delete(education);
        log.info("Deleted education record id={} for candidate userId={} in tenant={}", educationId, userId, tenantId);
    }

    /**
     * Deletes all education records for a candidate (e.g. for DPDP right to erasure).
     */
    public void deleteAllByUserId(UUID userId, String tenantId) {
        candidateEducationRepository.deleteByUserIdAndTenantId(userId, tenantId);
        log.info("Deleted all education records for userId={} in tenant={}", userId, tenantId);
    }

    private CandidateEducationResponse toResponse(CandidateEducation education) {
        return CandidateEducationResponse.from(education);
    }
}
