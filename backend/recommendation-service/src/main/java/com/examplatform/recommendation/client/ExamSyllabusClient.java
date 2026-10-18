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

package com.examplatform.recommendation.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-service client interface to retrieve examination syllabus scope and blueprint weights.
 */
public interface ExamSyllabusClient {
    List<ExamSyllabusNodeDto> getSyllabusNodes(UUID examId);
    Map<Long, Double> getBlueprintWeights(UUID examId);
}
