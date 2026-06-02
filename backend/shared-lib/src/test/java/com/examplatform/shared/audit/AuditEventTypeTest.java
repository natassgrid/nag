package com.examplatform.shared.audit;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTypeTest {

    private static final Set<String> EXPECTED_VALUES = Set.of(
            "LOGIN", "LOGOUT", "ROLE_CHANGE", "DENIED_ACCESS", "ACCOUNT_LOCK",
            "KEY_REVOCATION", "CANDIDATE_PROFILE_CREATED", "QUESTION_CREATED",
            "QUESTION_STATE_TRANSITION", "PAPER_GENERATED", "PAPER_APPROVED",
            "SESSION_STARTED", "SESSION_SUBMITTED", "EVALUATION_CREATED",
            "RESULT_PUBLISHED", "CONFIG_CHANGED", "TAMPER_ATTEMPT"
    );

    @Test
    void allRequiredEventTypesArePresent() {
        Set<String> actual = Arrays.stream(AuditEventType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(EXPECTED_VALUES, actual,
                "AuditEventType enum must contain exactly the platform-defined event types");
    }

    @Test
    void enumCountMatchesSpecification() {
        assertEquals(17, AuditEventType.values().length);
    }

    @Test
    void valueOfWorksForEachType() {
        for (String name : EXPECTED_VALUES) {
            assertDoesNotThrow(() -> AuditEventType.valueOf(name),
                    "AuditEventType.valueOf should not throw for: " + name);
        }
    }
}
