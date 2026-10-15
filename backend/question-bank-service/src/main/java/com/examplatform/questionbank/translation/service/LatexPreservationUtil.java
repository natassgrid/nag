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

package com.examplatform.questionbank.translation.service;

import java.util.List;

/**
 * Backward compatibility bridge for LaTeX preservation utility.
 *
 * @deprecated Moved to {@link com.examplatform.shared.ai.latex.LatexPreservationUtil}.
 */
@Deprecated
public final class LatexPreservationUtil {

    private LatexPreservationUtil() {
        // Utility class
    }

    public record MaskResult(String maskedText, List<String> preservedTokens) {}

    public static MaskResult mask(String text) {
        var res = com.examplatform.shared.ai.latex.LatexPreservationUtil.mask(text);
        return new MaskResult(res.maskedText(), res.preservedTokens());
    }

    public static String unmask(String translatedText, List<String> preservedTokens) {
        return com.examplatform.shared.ai.latex.LatexPreservationUtil.unmask(translatedText, preservedTokens);
    }
}
