package com.examplatform.shared.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successWithData_hasCorrectStatusAndData() {
        ApiResponse<String> response = ApiResponse.success("hello", "OK");

        assertEquals("success", response.getStatus());
        assertEquals("hello", response.getData());
        assertEquals("OK", response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void successWithoutData_hasNullData() {
        ApiResponse<String> response = ApiResponse.success("No content");

        assertEquals("success", response.getStatus());
        assertNull(response.getData());
    }

    @Test
    void errorResponse_hasErrorStatusAndNullData() {
        ApiResponse<Object> response = ApiResponse.error("Something went wrong");

        assertEquals("error", response.getStatus());
        assertNull(response.getData());
        assertEquals("Something went wrong", response.getMessage());
    }

    @Test
    void timestampIsAlwaysPopulated() {
        ApiResponse<Void> response = ApiResponse.error("err");
        assertNotNull(response.getTimestamp());
    }
}
