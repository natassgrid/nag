// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import java.util.List;
import java.util.UUID;

public record BulkDeleteMicroLessonsRequest(
        List<UUID> ids
) {}
