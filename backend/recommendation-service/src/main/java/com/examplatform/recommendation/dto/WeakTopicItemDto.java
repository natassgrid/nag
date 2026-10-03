// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.util.List;

public record WeakTopicItemDto(
    String topicName,
    double currentAccuracy,
    List<String> subtopicsToRevise
) {}
