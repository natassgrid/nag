package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.ShiftAssignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of ShiftAssignmentClient for local development.
 * In production, this would call the examination-service REST API.
 */
@Slf4j
@Component
public class ShiftAssignmentClientImpl implements ShiftAssignmentClient {

    @Override
    public ShiftAssignment getShiftAssignment(UUID candidateId, UUID examId, UUID shiftId, String tenantId) {
        log.info("[STUB] Getting shift assignment: candidate={}, exam={}, shift={}, tenant={}",
                candidateId, examId, shiftId, tenantId);
        return new ShiftAssignment(UUID.randomUUID(), null, 180, 0);
    }
}
