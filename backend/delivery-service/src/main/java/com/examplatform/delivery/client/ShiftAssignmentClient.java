package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.ShiftAssignment;

import java.util.UUID;

/**
 * Client interface for retrieving shift assignment details from the examination-service.
 * Implementations may use REST (WebClient/RestClient) or gRPC for inter-service communication.
 */
public interface ShiftAssignmentClient {

    /**
     * Retrieve the shift assignment for a candidate within a specific exam and shift.
     *
     * @param candidateId the candidate's unique identifier
     * @param examId      the examination identifier
     * @param shiftId     the shift identifier
     * @param tenantId    the tenant (examination authority) identifier
     * @return the shift assignment details including paper reference and duration
     */
    ShiftAssignment getShiftAssignment(UUID candidateId, UUID examId, UUID shiftId, String tenantId);
}
