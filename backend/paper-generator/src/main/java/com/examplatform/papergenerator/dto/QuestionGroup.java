/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.papergenerator.dto;

import java.util.List;
import java.util.UUID;

/**
 * Represents a group of comprehension/case study questions linked to a common stimulus passage.
 */
public record QuestionGroup(
        UUID passageId,
        List<UUID> questionIds
) {}
