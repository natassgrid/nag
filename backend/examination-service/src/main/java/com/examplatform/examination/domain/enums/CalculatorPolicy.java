/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.examination.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CalculatorPolicy {
    NONE,
    BASIC,
    SCIENTIFIC;

    @JsonCreator
    public static CalculatorPolicy fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "NONE", "DISALLOWED", "NO_CALCULATOR", "NOT_PERMITTED" -> NONE;
            case "BASIC", "VIRTUAL_BASIC", "STANDARD_VIRTUAL" -> BASIC;
            case "SCIENTIFIC", "VIRTUAL_SCIENTIFIC", "SCIENTIFIC_VIRTUAL" -> SCIENTIFIC;
            default -> {
                for (CalculatorPolicy policy : values()) {
                    if (policy.name().equalsIgnoreCase(normalized)) {
                        yield policy;
                    }
                }
                throw new IllegalArgumentException("Unknown CalculatorPolicy: " + value);
            }
        };
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
