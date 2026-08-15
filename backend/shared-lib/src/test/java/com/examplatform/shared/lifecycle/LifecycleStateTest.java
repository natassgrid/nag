/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.shared.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleStateTest {

    // -----------------------------------------------------------------------
    // QuestionState
    // -----------------------------------------------------------------------

    @Test
    void questionState_hasAllRequiredValues() {
        assertDoesNotThrow(() -> QuestionState.valueOf("DRAFT"));
        assertDoesNotThrow(() -> QuestionState.valueOf("REVIEW"));
        assertDoesNotThrow(() -> QuestionState.valueOf("APPROVED"));
        assertDoesNotThrow(() -> QuestionState.valueOf("PUBLISHED"));
        assertDoesNotThrow(() -> QuestionState.valueOf("ARCHIVED"));
        assertEquals(5, QuestionState.values().length);
    }

    // -----------------------------------------------------------------------
    // TranslationState
    // -----------------------------------------------------------------------

    @Test
    void translationState_hasAllRequiredValues() {
        assertDoesNotThrow(() -> TranslationState.valueOf("DRAFT"));
        assertDoesNotThrow(() -> TranslationState.valueOf("APPROVED"));
        assertDoesNotThrow(() -> TranslationState.valueOf("STALE"));
        assertEquals(3, TranslationState.values().length);
    }

    // -----------------------------------------------------------------------
    // PaperState
    // -----------------------------------------------------------------------

    @Test
    void paperState_hasAllRequiredValues() {
        assertDoesNotThrow(() -> PaperState.valueOf("DRAFT"));
        assertDoesNotThrow(() -> PaperState.valueOf("APPROVED"));
        assertDoesNotThrow(() -> PaperState.valueOf("ENCRYPTED"));
        assertEquals(3, PaperState.values().length);
    }

    // -----------------------------------------------------------------------
    // SessionState
    // -----------------------------------------------------------------------

    @Test
    void sessionState_hasAllRequiredValues() {
        assertDoesNotThrow(() -> SessionState.valueOf("ACTIVE"));
        assertDoesNotThrow(() -> SessionState.valueOf("SUBMITTED"));
        assertDoesNotThrow(() -> SessionState.valueOf("EXPIRED"));
        assertEquals(3, SessionState.values().length);
    }

    // -----------------------------------------------------------------------
    // EvaluationState
    // -----------------------------------------------------------------------

    @Test
    void evaluationState_hasAllRequiredValues() {
        assertDoesNotThrow(() -> EvaluationState.valueOf("PENDING"));
        assertDoesNotThrow(() -> EvaluationState.valueOf("AUTO_EVALUATED"));
        assertDoesNotThrow(() -> EvaluationState.valueOf("MANUAL_EVALUATED"));
        assertDoesNotThrow(() -> EvaluationState.valueOf("ARBITRATION"));
        assertDoesNotThrow(() -> EvaluationState.valueOf("FINALIZED"));
        assertEquals(5, EvaluationState.values().length);
    }
}
