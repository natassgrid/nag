/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.extraction.ai;

import com.examplatform.questionbank.dto.QuestionOption;

import java.util.List;

/**
 * Normalizes question option identifiers across Indian competitive examination formats.
 * Handles digits (1, 2, 3, 4), Latin letters (a, b, c, d), Roman numerals (i, ii, iii, iv),
 * and Devanagari numerals/characters (क, ख, ग, घ and अ, ब, स, द) into canonical "A", "B", "C", "D".
 * Aligns English and Hindi option streams.
 */
public final class OptionIdNormalizer {

    private OptionIdNormalizer() {}

    /**
     * Cleans extraneous brackets, parentheses, dots, colons, or whitespace around option tokens.
     *
     * @param token raw option token (e.g. "(1)", "[क]", "a.")
     * @return cleaned alphanumeric / Devanagari token
     */
    public static String cleanToken(String token) {
        if (token == null) {
            return "";
        }
        String t = token.trim();
        t = t.replaceAll("^[\\[\\(\\{]+", "")
             .replaceAll("[\\]\\)\\}]+$", "")
             .replaceAll("[\\.\\:\\-\\s]+$", "")
             .trim();
        return t;
    }

    /**
     * Maps any recognized option indicator to canonical uppercase Latin ID ("A", "B", "C", "D").
     *
     * @param token raw or partially cleaned token
     * @return canonical ID e.g. "A", "B", "C", "D"
     */
    public static String normalize(String token) {
        if (token == null || token.isBlank()) {
            return "A";
        }
        String clean = cleanToken(token);
        String upper = clean.toUpperCase();

        return switch (upper) {
            case "1", "A", "I", "क", "अ" -> "A";
            case "2", "B", "II", "ख", "ब" -> "B";
            case "3", "C", "III", "ग", "स" -> "C";
            case "4", "D", "IV", "घ", "द" -> "D";
            default -> {
                if (upper.length() == 1 && upper.charAt(0) >= 'A' && upper.charAt(0) <= 'Z') {
                    yield upper;
                }
                yield clean.isEmpty() ? "A" : upper;
            }
        };
    }

    /**
     * Produces a positional option ID starting at 0 -> "A", 1 -> "B", etc.
     *
     * @param index 0-based option index
     * @return canonical Latin letter ID
     */
    public static String positionalId(int index) {
        return switch (index) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            case 4 -> "E";
            case 5 -> "F";
            default -> String.valueOf((char) ('A' + index));
        };
    }

    /**
     * Normalizes all option IDs, Hindi option IDs, and answer keys on an extracted item payload.
     * Guarantees that English and Hindi option lists have aligned ID sets when list sizes match.
     *
     * @param item the extracted item payload to normalize in-place
     */
    public static void normalizeQuestionOptions(LlmPageExtractionResult.ExtractedItemPayload item) {
        if (item == null) {
            return;
        }

        List<QuestionOption> options = item.getOptions();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                QuestionOption opt = options.get(i);
                if (opt.getId() == null || opt.getId().isBlank()) {
                    opt.setId(positionalId(i));
                } else {
                    opt.setId(normalize(opt.getId()));
                }
            }
        }

        List<QuestionOption> hindiOptions = item.getHindiOptions();
        if (hindiOptions != null) {
            for (int i = 0; i < hindiOptions.size(); i++) {
                QuestionOption hOpt = hindiOptions.get(i);
                if (hOpt.getId() == null || hOpt.getId().isBlank()) {
                    hOpt.setId(positionalId(i));
                } else {
                    hOpt.setId(normalize(hOpt.getId()));
                }
            }
        }

        // Align IDs between English and Hindi when option counts match
        if (options != null && hindiOptions != null && options.size() == hindiOptions.size()) {
            for (int i = 0; i < options.size(); i++) {
                hindiOptions.get(i).setId(options.get(i).getId());
            }
        }

        // Normalize answerKey and sync correct flags
        if (item.getAnswerKey() != null && !item.getAnswerKey().isBlank()) {
            String canonicalKey = normalize(item.getAnswerKey());
            item.setAnswerKey(canonicalKey);
            if (options != null) {
                for (QuestionOption opt : options) {
                    if (opt.getId() != null && opt.getId().equalsIgnoreCase(canonicalKey)) {
                        opt.setCorrect(true);
                    }
                }
            }
            if (hindiOptions != null) {
                for (QuestionOption hOpt : hindiOptions) {
                    if (hOpt.getId() != null && hOpt.getId().equalsIgnoreCase(canonicalKey)) {
                        hOpt.setCorrect(true);
                    }
                }
            }
        } else if (options != null) {
            // If answerKey was null but an option is marked correct, adopt that ID
            for (QuestionOption opt : options) {
                if (opt.isCorrect() && opt.getId() != null) {
                    item.setAnswerKey(opt.getId());
                    break;
                }
            }
        }
    }
}
