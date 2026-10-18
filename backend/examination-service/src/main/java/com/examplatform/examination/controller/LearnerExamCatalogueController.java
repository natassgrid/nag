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

package com.examplatform.examination.controller;

import com.examplatform.examination.dto.ExamCatalogueItemResponse;
import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.constants.SharedConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for learner exam catalogue discovery.
 * Serves published examinations for onboarding candidate target exam selection.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/learner/exams")
@RequiredArgsConstructor
public class LearnerExamCatalogueController {

    private final ExaminationService examinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExamCatalogueItemResponse>>> getCatalogue(
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, required = false) String tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        log.debug("Learner exam catalogue request: search={}, category={}, tenant={}", search, category, effectiveTenant);

        Page<ExamCatalogueItemResponse> catalogue =
                examinationService.listCatalogue(effectiveTenant, search, category, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(catalogue, "Published examination catalogue retrieved successfully"));
    }
}
