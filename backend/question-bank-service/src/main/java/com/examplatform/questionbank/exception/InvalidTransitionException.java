package com.examplatform.questionbank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a question lifecycle state transition is not valid
 * according to the FSM rules.
 *
 * Validates: Requirements 4.6
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidTransitionException extends RuntimeException {

    private final String currentState;
    private final String targetState;

    public InvalidTransitionException(String currentState, String targetState) {
        super(String.format("Invalid transition from '%s' to '%s'", currentState, targetState));
        this.currentState = currentState;
        this.targetState = targetState;
    }
}
