// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepByStepBreakdownDto(
        @JsonAlias({"problemStatement"})
        String questionStem,
        List<String> steps,
        String finalAnswer,
        List<StepItemDto> structuredSteps,
        TrapAlertDto structuredTrapAlert,
        List<FormulaEntryDto> structuredFormulas,
        List<String> keyTakeaways,
        List<QuickCheckItemDto> quickCheckItems
) implements Serializable {

    public StepByStepBreakdownDto(String questionStem, List<String> steps, String finalAnswer) {
        this(questionStem, steps, finalAnswer, null, null, null, null, null);
    }

    @JsonProperty(value = "problemStatement", access = JsonProperty.Access.READ_ONLY)
    public String getProblemStatement() {
        return questionStem;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StepByStepBreakdownDto fromJson(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof StepByStepBreakdownDto dto) {
            return dto;
        }
        if (raw instanceof List<?> list) {
            List<String> rawSteps = new ArrayList<>();
            List<StepItemDto> structured = new ArrayList<>();
            parseStepsList(list, rawSteps, structured);
            return new StepByStepBreakdownDto(
                    null,
                    rawSteps,
                    null,
                    structured.isEmpty() ? null : structured,
                    null,
                    null,
                    null,
                    null
            );
        }
        if (raw instanceof Map<?, ?> map) {
            String questionStem = stringVal(map, "questionStem");
            if (questionStem == null) {
                questionStem = stringVal(map, "problemStatement");
            }
            String finalAnswer = stringVal(map, "finalAnswer");

            List<String> rawSteps = new ArrayList<>();
            List<StepItemDto> structured = new ArrayList<>();
            Object stepsObj = map.get("steps");
            if (stepsObj instanceof List<?> stepsList) {
                parseStepsList(stepsList, rawSteps, structured);
            }

            Object structuredStepsObj = map.get("structuredSteps");
            if (structuredStepsObj instanceof List<?> explicitList && !explicitList.isEmpty()) {
                List<StepItemDto> explicitStructured = new ArrayList<>();
                int idx = 1;
                for (Object item : explicitList) {
                    StepItemDto parsed = toStepItemDto(item, idx++);
                    if (parsed != null) {
                        explicitStructured.add(parsed);
                    }
                }
                if (!explicitStructured.isEmpty()) {
                    structured = explicitStructured;
                }
            }

            TrapAlertDto structuredTrap = toTrapAlertDto(map.get("structuredTrapAlert"));

            List<FormulaEntryDto> structuredFormulas = null;
            Object formulasObj = map.get("structuredFormulas");
            if (formulasObj instanceof List<?> fList) {
                structuredFormulas = new ArrayList<>();
                int idx = 1;
                for (Object item : fList) {
                    FormulaEntryDto f = toFormulaEntryDto(item, idx++);
                    if (f != null) {
                        structuredFormulas.add(f);
                    }
                }
            }

            List<String> keyTakeaways = toStringList(map.get("keyTakeaways"));

            List<QuickCheckItemDto> quickCheckItems = null;
            Object qcObj = map.get("quickCheckItems");
            if (qcObj instanceof List<?> qcList) {
                quickCheckItems = new ArrayList<>();
                for (Object item : qcList) {
                    QuickCheckItemDto qc = toQuickCheckItemDto(item);
                    if (qc != null) {
                        quickCheckItems.add(qc);
                    }
                }
            }

            return new StepByStepBreakdownDto(
                    questionStem,
                    rawSteps,
                    finalAnswer,
                    structured.isEmpty() ? null : structured,
                    structuredTrap,
                    structuredFormulas,
                    keyTakeaways,
                    quickCheckItems
            );
        }
        return null;
    }

    static void parseStepsList(List<?> list, List<String> rawSteps, List<StepItemDto> structured) {
        int idx = 1;
        for (Object item : list) {
            if (item == null) continue;
            if (item instanceof String s) {
                rawSteps.add(s);
            } else {
                StepItemDto dto = toStepItemDto(item, idx);
                if (dto != null) {
                    structured.add(dto);
                    StringBuilder line = new StringBuilder();
                    if (dto.explanation() != null && !dto.explanation().isBlank()) {
                        line.append(dto.explanation());
                    } else if (dto.title() != null && !dto.title().isBlank()) {
                        line.append(dto.title());
                    }
                    if (dto.formula() != null && !dto.formula().isBlank() && !line.toString().contains(dto.formula())) {
                        if (!line.isEmpty()) {
                            line.append(" ");
                        }
                        line.append(dto.formula());
                    }
                    rawSteps.add(line.toString());
                }
            }
            idx++;
        }
    }

    static StepItemDto toStepItemDto(Object item, int defaultIndex) {
        if (item instanceof StepItemDto dto) {
            return dto;
        }
        if (item instanceof Map<?, ?> m) {
            int stepNum = intVal(m, "stepNumber", defaultIndex);
            String title = stringVal(m, "title");
            if (title == null || title.isBlank()) {
                title = "Step " + stepNum;
            }
            String explanation = stringVal(m, "explanation");
            String formula = stringVal(m, "formula");
            String tip = stringVal(m, "tip");
            return new StepItemDto(stepNum, title, explanation != null ? explanation : "", formula, tip);
        }
        return null;
    }

    static TrapAlertDto toTrapAlertDto(Object obj) {
        if (obj == null) return null;
        if (obj instanceof TrapAlertDto dto) return dto;
        if (obj instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : new TrapAlertDto("Exam Trap & Common Pitfall", trimmed, trimmed, null);
        }
        if (obj instanceof Map<?, ?> m) {
            return new TrapAlertDto(
                    stringVal(m, "title"),
                    stringVal(m, "description"),
                    stringVal(m, "commonMistake"),
                    stringVal(m, "correctApproach")
            );
        }
        return null;
    }

    static FormulaEntryDto toFormulaEntryDto(Object obj, int index) {
        if (obj == null) return null;
        if (obj instanceof FormulaEntryDto dto) return dto;
        if (obj instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) return null;
            String name = "Formula " + index;
            String latex = trimmed;
            String desc = trimmed;

            int colonIdx = trimmed.indexOf(':');
            int firstDollarIdx = trimmed.indexOf('$');
            int firstSlashIdx = trimmed.indexOf('\\');

            boolean hasColon = colonIdx > 0;
            boolean colonBeforeDollar = firstDollarIdx == -1 || colonIdx < firstDollarIdx;
            boolean colonBeforeSlash = firstSlashIdx == -1 || colonIdx < firstSlashIdx;

            if (hasColon && colonBeforeDollar && colonBeforeSlash) {
                String namePart = trimmed.substring(0, colonIdx).trim();
                String latexPart = trimmed.substring(colonIdx + 1).trim();
                if (!namePart.isEmpty() && !namePart.contains("$") && !namePart.contains("\\")) {
                    name = namePart;
                    latex = latexPart;
                    desc = namePart;
                }
            }
            return new FormulaEntryDto(name, latex, desc, List.of());
        }
        if (obj instanceof Map<?, ?> m) {
            String name = firstNonBlank(stringVal(m, "name"), stringVal(m, "title"));
            String latex = firstNonBlank(stringVal(m, "latex"), stringVal(m, "formula"));
            String desc = firstNonBlank(stringVal(m, "description"), stringVal(m, "usage"));
            List<String> vars = toStringList(m.get("variables"));
            return new FormulaEntryDto(name, latex, desc, vars);
        }
        return null;
    }

    static QuickCheckItemDto toQuickCheckItemDto(Object obj) {
        if (obj == null) return null;
        if (obj instanceof QuickCheckItemDto dto) return dto;
        if (obj instanceof Map<?, ?> m) {
            String question = stringVal(m, "question");
            List<String> options = toStringList(m.get("options"));
            String correctAnswer = firstNonBlank(stringVal(m, "correctAnswer"), stringVal(m, "answer"));
            String explanation = stringVal(m, "explanation");
            return new QuickCheckItemDto(question, options, correctAnswer, explanation);
        }
        return null;
    }

    static List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> res = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    res.add(String.valueOf(item));
                }
            }
            return res;
        }
        return null;
    }

    static String stringVal(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    static int intVal(Map<?, ?> map, String key, int fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }
}
