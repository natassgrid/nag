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

package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.CandidateExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of CandidateProfileClient for local development.
 * In production, this would call the candidate-service REST API.
 */
@Slf4j
@Component
public class CandidateProfileClientImpl implements CandidateProfileClient {

    @Override
    public CandidateExtension getExtension(UUID candidateId, String tenantId) {
        log.info("[STUB] Getting candidate extension: candidate={}, tenant={}", candidateId, tenantId);
        return null; // No extension by default
    }
}
