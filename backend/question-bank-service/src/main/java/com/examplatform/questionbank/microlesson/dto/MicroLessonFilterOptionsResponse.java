// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import java.util.List;

public record MicroLessonFilterOptionsResponse(
        List<String> subjects,
        List<String> topics,
        List<String> statuses
) {}
