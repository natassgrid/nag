/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.papergenerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExaminationLookupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExaminationLookupService lookupService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("findExamNames returns empty map when examIds is null or empty")
    void testFindExamNames_EmptyInput() {
        assertThat(lookupService.findExamNames(null)).isEmpty();
        assertThat(lookupService.findExamNames(Collections.emptySet())).isEmpty();
    }

    @Test
    @DisplayName("findExamNames returns empty map when examination table does not exist")
    void testFindExamNames_TableNotAvailable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("examination_service"), eq("examination")))
                .thenReturn(0);

        UUID examId = UUID.randomUUID();
        Map<UUID, String> result = lookupService.findExamNames(Set.of(examId));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findExamNames successfully queries and returns examination names when table is present")
    void testFindExamNames_Success() throws SQLException {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("examination_service"), eq("examination")))
                .thenReturn(1);

        UUID examId1 = UUID.randomUUID();
        UUID examId2 = UUID.randomUUID();

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(2);
            ResultSet rs1 = mock(ResultSet.class);
            when(rs1.getObject("id")).thenReturn(examId1);
            when(rs1.getString("name")).thenReturn("National Civil Services Exam");
            rch.processRow(rs1);

            ResultSet rs2 = mock(ResultSet.class);
            when(rs2.getObject("id")).thenReturn(examId2);
            when(rs2.getString("name")).thenReturn("State Eligibility Test");
            rch.processRow(rs2);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));

        Map<UUID, String> result = lookupService.findExamNames(Set.of(examId1, examId2));

        assertThat(result).hasSize(2);
        assertThat(result.get(examId1)).isEqualTo("National Civil Services Exam");
        assertThat(result.get(examId2)).isEqualTo("State Eligibility Test");
    }

    @Test
    @DisplayName("findShiftNames returns empty map when shiftIds is null or empty")
    void testFindShiftNames_EmptyInput() {
        assertThat(lookupService.findShiftNames(null)).isEmpty();
        assertThat(lookupService.findShiftNames(Collections.emptySet())).isEmpty();
    }

    @Test
    @DisplayName("findShiftNames returns empty map when shift table does not exist")
    void testFindShiftNames_TableNotAvailable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("examination_service"), eq("exam_shift")))
                .thenReturn(0);

        Map<String, String> result = lookupService.findShiftNames(Set.of(UUID.randomUUID().toString()));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findShiftNames queries and maps shift names or fallback to Shift number")
    void testFindShiftNames_Success() throws SQLException {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("examination_service"), eq("exam_shift")))
                .thenReturn(1);

        UUID shiftId1 = UUID.randomUUID();
        UUID shiftId2 = UUID.randomUUID();

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(2);
            ResultSet rs1 = mock(ResultSet.class);
            when(rs1.getObject("id")).thenReturn(shiftId1);
            when(rs1.getString("shift_name")).thenReturn("Morning Shift");
            when(rs1.getInt("shift_number")).thenReturn(1);
            rch.processRow(rs1);

            ResultSet rs2 = mock(ResultSet.class);
            when(rs2.getObject("id")).thenReturn(shiftId2);
            when(rs2.getString("shift_name")).thenReturn(null);
            when(rs2.getInt("shift_number")).thenReturn(2);
            rch.processRow(rs2);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));

        Map<String, String> result = lookupService.findShiftNames(Set.of(shiftId1.toString(), shiftId2.toString()));

        assertThat(result).hasSize(2);
        assertThat(result.get(shiftId1.toString())).isEqualTo("Morning Shift");
        assertThat(result.get(shiftId2.toString())).isEqualTo("Shift 2");
    }
}
