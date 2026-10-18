/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.shared.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EnrollmentRef Contract & Serialization Tests")
class EnrollmentRefTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("EnrollmentRef factory methods, equality, and Jackson serialization")
    void testEnrollmentRef() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        EnrollmentRef ref = EnrollmentRef.of(candidateId, enrollmentId, examId);
        assertThat(ref.candidateId()).isEqualTo(candidateId);
        assertThat(ref.enrollmentId()).isEqualTo(enrollmentId);
        assertThat(ref.examId()).isEqualTo(examId);

        EnrollmentRef stringRef = EnrollmentRef.of(
                candidateId.toString(),
                enrollmentId.toString(),
                examId.toString()
        );
        assertThat(stringRef).isEqualTo(ref);

        String json = objectMapper.writeValueAsString(ref);
        EnrollmentRef deserialized = objectMapper.readValue(json, EnrollmentRef.class);
        assertThat(deserialized).isEqualTo(ref);
    }
}
