/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.client;

import com.examplatform.candidate.dto.PublishedExamDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-service client interface to examination-service.
 * Pluggable across microservices (REST/gRPC) and macro/monolith (in-process/DB) modes.
 */
public interface ExaminationClient {
    List<PublishedExamDto> getPublishedExams(String tenantId);
    Optional<PublishedExamDto> getExam(UUID examId, String tenantId);
}
