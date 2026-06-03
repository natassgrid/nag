package com.examplatform.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Generic response envelope returned by every API endpoint in the platform.
 *
 * <pre>
 * {
 *   "status":    "SUCCESS" | "ERROR",
 *   "message":   "Human-readable summary",
 *   "data":      { ... },        // present on success; absent on error
 *   "timestamp": "2024-01-01T00:00:00"
 * }
 * </pre>
 *
 * @param <T> the type of the response payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    /** "SUCCESS" or "ERROR". */
    private final String status;

    /** Short human-readable summary of the outcome. */
    private final String message;

    /** Response payload — null on error responses. */
    private final T data;

    /** UTC timestamp of when the response was created. */
    private final LocalDateTime timestamp;

    // -----------------------------------------------------------------------
    // Private constructor — use static factory methods
    // -----------------------------------------------------------------------

    private ApiResponse(String status, String message, T data) {
        this.status    = status;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    // -----------------------------------------------------------------------
    // Static factories
    // -----------------------------------------------------------------------

    /**
     * Build a successful response with a payload.
     *
     * @param data response payload
     * @param <T>  payload type
     * @return success envelope with default "OK" message
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data);
    }

    /**
     * Build a successful response with a payload and custom message.
     *
     * @param data    response payload
     * @param message human-readable success message
     * @param <T>     payload type
     * @return success envelope
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    /**
     * Build an error response.
     *
     * @param message human-readable error summary
     * @param <T>     payload type
     * @return error envelope with {@code data = null}
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("ERROR", message, null);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{status='" + status + "', message='" + message + "', timestamp=" + timestamp + '}';
    }
}
