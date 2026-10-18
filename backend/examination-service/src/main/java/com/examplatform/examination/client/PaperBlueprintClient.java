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

package com.examplatform.examination.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for interacting with paper-generator service to retrieve blueprint structures
 * and execute sufficiency verification.
 */
public interface PaperBlueprintClient {

    record BlueprintRuleInfo(
            UUID id,
            Long subjectId,
            Long topicId,
            Long subtopicId,
            String difficulty,
            String cognitiveLevel,
            int questionCount,
            Double marksOverride
    ) {}

    record BlueprintSectionInfo(
            UUID id,
            String name,
            int orderIndex,
            Long subjectId,
            List<BlueprintRuleInfo> rules
    ) {}

    record BlueprintInfo(
            UUID id,
            String code,
            String name,
            int version,
            String status,
            List<BlueprintSectionInfo> sections
    ) {}

    record SufficiencyResult(
            boolean feasible,
            int totalQuestionsNeeded,
            int totalQuestionsAvailable,
            int deficitRuleCount,
            String summary
    ) {}

    Optional<BlueprintInfo> getBlueprint(UUID blueprintId, String tenantId);

    SufficiencyResult checkSufficiency(UUID blueprintId, UUID examId, int shifts, String tenantId);
}
