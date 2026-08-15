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

package com.examplatform.delivery.service;

import com.examplatform.delivery.client.CandidateProfileClient;
import com.examplatform.delivery.dto.CandidateExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for determining disability-based time extensions for candidates.
 * Retrieves extension configuration from the candidate-service and returns the
 * additional time in minutes that should be added to the exam session duration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisabilityExtensionService {

    private final CandidateProfileClient candidateProfileClient;

    /**
     * Gets the extra time in minutes that should be granted to a candidate based
     * on their disability accommodation configuration.
     *
     * @param candidateId the candidate's unique identifier
     * @param tenantId    the tenant (examination authority) identifier
     * @return extra time in minutes, or 0 if no extension is configured
     */
    public int getExtraTimeMinutes(UUID candidateId, String tenantId) {
        try {
            CandidateExtension extension = candidateProfileClient.getExtension(candidateId, tenantId);
            if (extension == null) {
                return 0;
            }
            int extraMinutes = extension.getExtraTimeMinutes();
            if (extraMinutes > 0) {
                log.info("Disability extension of {} minutes applied for candidate {} (type: {})",
                        extraMinutes, candidateId, extension.getDisabilityType());
            }
            return Math.max(extraMinutes, 0);
        } catch (Exception e) {
            log.error("Failed to retrieve disability extension for candidate {}: {}",
                    candidateId, e.getMessage(), e);
            return 0;
        }
    }
}
