package com.examplatform.examination.exception;

/**
 * Thrown when a seat allocation would cause availableSeats to drop below zero
 * or when a duplicate (shift, centre) allocation is attempted (Req 7b.6).
 */
public class SeatAllocationException extends RuntimeException {
    public SeatAllocationException(String message) {
        super(message);
    }
}
