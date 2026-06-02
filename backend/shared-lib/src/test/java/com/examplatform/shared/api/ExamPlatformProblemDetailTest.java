package com.examplatform.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ExamPlatformProblemDetailTest {

    @Test
    void build_withAllFields_populatesCorrectly() {
        ProblemDetail problem = ExamPlatformProblemDetail
                .forStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .type(URI.create("https://errors.example.com/invalid-transition"))
                .title("Invalid State Transition")
                .detail("Cannot move from APPROVED to DRAFT")
                .instance(URI.create("/api/v1/questions/123/transition"))
                .property("currentState", "APPROVED")
                .property("traceId", "abc-123")
                .build();

        assertEquals(422, problem.getStatus());
        assertEquals("Invalid State Transition", problem.getTitle());
        assertEquals("Cannot move from APPROVED to DRAFT", problem.getDetail());
        assertEquals("APPROVED", problem.getProperties().get("currentState"));
        assertEquals("abc-123", problem.getProperties().get("traceId"));
    }

    @Test
    void build_withStatusCode_resolves() {
        ProblemDetail problem = ExamPlatformProblemDetail
                .forStatus(404)
                .title("Not Found")
                .build();

        assertEquals(404, problem.getStatus());
        assertEquals("Not Found", problem.getTitle());
    }

    @Test
    void build_withMinimalFields_doesNotThrow() {
        assertDoesNotThrow(() ->
                ExamPlatformProblemDetail.forStatus(HttpStatus.BAD_REQUEST).build()
        );
    }
}
