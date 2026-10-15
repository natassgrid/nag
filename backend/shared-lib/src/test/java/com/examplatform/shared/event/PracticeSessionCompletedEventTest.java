// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PracticeSessionCompletedEvent Serialization & Contract Tests")
class PracticeSessionCompletedEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Serializes and deserializes correctly via Jackson")
    void testSerializationAndDeserialization() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID practiceSetId = UUID.randomUUID();
        String topicJson = "{\"Physics\":{\"correct\":5,\"total\":10}}";
        String subtopicJson = "[{\"subtopicId\":100,\"subtopicName\":\"Kinematics\",\"totalQuestions\":5,\"totalCorrect\":4}]";

        PracticeSessionCompletedEvent event = new PracticeSessionCompletedEvent(
                sessionId,
                candidateId,
                practiceSetId,
                7,
                2,
                1,
                24,
                30,
                topicJson,
                subtopicJson
        );

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains(sessionId.toString());
        assertThat(json).contains(candidateId.toString());
        assertThat(json).contains(practiceSetId.toString());

        PracticeSessionCompletedEvent deserialized = objectMapper.readValue(json, PracticeSessionCompletedEvent.class);
        assertThat(deserialized.sessionId()).isEqualTo(sessionId);
        assertThat(deserialized.candidateId()).isEqualTo(candidateId);
        assertThat(deserialized.practiceSetId()).isEqualTo(practiceSetId);
        assertThat(deserialized.correctCount()).isEqualTo(7);
        assertThat(deserialized.incorrectCount()).isEqualTo(2);
        assertThat(deserialized.skippedCount()).isEqualTo(1);
        assertThat(deserialized.obtainedMarks()).isEqualTo(24);
        assertThat(deserialized.totalMarks()).isEqualTo(30);
        assertThat(deserialized.topicWiseBreakdown()).isEqualTo(topicJson);
        assertThat(deserialized.subtopicWiseBreakdown()).isEqualTo(subtopicJson);
    }
}
