// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockExamAccessValidator Full Mock Gating Unit Tests")
class MockExamAccessValidatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private MockExamAccessValidator validator;

    private static final UUID CANDIDATE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        validator = new MockExamAccessValidator(jdbcTemplate, objectMapper);
        lenient().when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class))).thenReturn(true);
    }

    @Test
    @DisplayName("Blocks full mock start with 422 GATEKEEPER_REQUIRED when Gatekeeper is pending")
    void testBlocksWhenGatekeeperPending() throws Exception {
        String json = """
            {
                "gatekeeperPassed": false,
                "conceptualGaps": [{"subtopicName": "Moment of Inertia"}],
                "applicationGaps": []
            }
            """;

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("milestone_graph_json")).thenReturn(json);

        doAnswer(inv -> {
            RowCallbackHandler rch = inv.getArgument(1);
            rch.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(any(String.class), any(RowCallbackHandler.class), eq(CANDIDATE_ID));

        assertThatThrownBy(() -> validator.validateCandidateAccess(CANDIDATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    org.assertj.core.api.Assertions.assertThat(rse.getReason()).contains("GATEKEEPER_REQUIRED");
                });
    }

    @Test
    @DisplayName("Permits full mock when Gatekeeper has been passed")
    void testPermitsWhenGatekeeperPassed() throws Exception {
        String json = """
            {
                "gatekeeperPassed": true,
                "conceptualGaps": [{"subtopicName": "Moment of Inertia"}],
                "applicationGaps": []
            }
            """;

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("milestone_graph_json")).thenReturn(json);

        doAnswer(inv -> {
            RowCallbackHandler rch = inv.getArgument(1);
            rch.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(any(String.class), any(RowCallbackHandler.class), eq(CANDIDATE_ID));

        assertThatNoException().isThrownBy(() -> validator.validateCandidateAccess(CANDIDATE_ID));
    }

    @Test
    @DisplayName("Permits full mock when candidate has no gaps")
    void testPermitsWhenNoGaps() throws Exception {
        String json = """
            {
                "gatekeeperPassed": false,
                "conceptualGaps": [],
                "applicationGaps": []
            }
            """;

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("milestone_graph_json")).thenReturn(json);

        doAnswer(inv -> {
            RowCallbackHandler rch = inv.getArgument(1);
            rch.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(any(String.class), any(RowCallbackHandler.class), eq(CANDIDATE_ID));

        assertThatNoException().isThrownBy(() -> validator.validateCandidateAccess(CANDIDATE_ID));
    }
}
