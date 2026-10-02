// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.event;

import java.util.UUID;

public record PracticeSessionCompletedEvent(
  UUID sessionId,
  UUID candidateId,
  UUID practiceSetId,
  int correctCount,
  int incorrectCount,
  int skippedCount,
  int obtainedMarks,
  int totalMarks,
  String topicWiseBreakdown
) {}
