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

package com.examplatform.shared.ai.latex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal validator for LaTeX and KaTeX mathematical notation.
 * Checks delimiter pairing ($ and $$), bracket/brace balancing ({}, []),
 * and LaTeX environments (\begin{env} ... \end{env}).
 */
public final class LatexSyntaxValidator {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\\\(begin|end)\\{([a-zA-Z0-9*]+)\\}");

    private LatexSyntaxValidator() {
        // Utility class
    }

    /**
     * Checks if the given text has valid LaTeX/KaTeX syntax.
     */
    public static boolean isValid(String text) {
        return validate(text).isEmpty();
    }

    /**
     * Validates that LaTeX expressions in the provided text have
     * syntactically sound delimiters and balanced brackets/environments.
     *
     * @param text expression or content to validate
     * @return list of validation errors, empty if completely valid
     */
    public static List<String> validate(String text) {
        List<String> errors = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return errors;
        }

        int len = text.length();

        // 1. Check balanced dollar delimiters (ignoring escaped ones: \$)
        int singleDollarCount = 0;
        int doubleDollarCount = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++; // skip escaped character
                continue;
            }
            if (c == '$') {
                if (i + 1 < len && text.charAt(i + 1) == '$') {
                    doubleDollarCount++;
                    i++;
                } else {
                    singleDollarCount++;
                }
            }
        }

        if (singleDollarCount % 2 != 0) {
            errors.add("Unbalanced inline math delimiter '$' in expression");
        }
        if (doubleDollarCount % 2 != 0) {
            errors.add("Unbalanced display math delimiter '$$' in expression");
        }

        // 2. Check balanced curly braces '{' and '}' and brackets '[' and ']'
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++; // skip escaped braces like \{ or \}
                continue;
            }
            if (c == '{' || c == '[') {
                stack.push(c);
            } else if (c == '}') {
                if (stack.isEmpty() || stack.pop() != '{') {
                    errors.add("Unbalanced or mismatched closing brace '}' in expression");
                    break;
                }
            } else if (c == ']') {
                if (stack.isEmpty() || stack.pop() != '[') {
                    errors.add("Unbalanced or mismatched closing bracket ']' in expression");
                    break;
                }
            }
        }
        if (!stack.isEmpty()) {
            char unclosed = stack.pop();
            errors.add("Unclosed delimiter '" + unclosed + "' in expression");
        }

        // 3. Check balanced environments \begin{...} and \end{...}
        Deque<String> envStack = new ArrayDeque<>();
        Matcher envMatcher = ENV_PATTERN.matcher(text);
        while (envMatcher.find()) {
            String command = envMatcher.group(1);
            String envName = envMatcher.group(2);
            if ("begin".equals(command)) {
                envStack.push(envName);
            } else if ("end".equals(command)) {
                if (envStack.isEmpty()) {
                    errors.add("Unmatched \\end{" + envName + "} without corresponding \\begin");
                } else {
                    String expectedEnv = envStack.pop();
                    if (!expectedEnv.equals(envName)) {
                        errors.add("Mismatched environment: expected \\end{" + expectedEnv + "} but found \\end{" + envName + "}");
                    }
                }
            }
        }
        while (!envStack.isEmpty()) {
            errors.add("Unclosed LaTeX environment \\begin{" + envStack.pop() + "}");
        }

        return errors;
    }
}
