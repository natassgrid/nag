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

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.microlesson.ai.agent.DeterministicJsonRepairEngine;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LiteLLM AI Extraction Client for Question Paper PDF pages.
 * Handles multimodal extraction via LiteLLM gateway routes (vision/text)
 * and provides a robust deterministic regex/heuristic fallback for offline development,
 * CI/CD pipelines, and network disconnect resilience.
 */
@Service
@Slf4j
public class LiteLlmPageExtractionClient {

    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    private static final Pattern QUESTION_HEADER_PATTERN = Pattern.compile(
            "(?m)^[\\s]*(?:Q(?:uestion)?\\.?\\s*)?(\\d{1,3})[\\.\\)]\\s+"
    );

    private static final Pattern OPTION_LINE_PATTERN = Pattern.compile(
            "(?m)^[\\s]*[\\(\\[]?([A-Da-d1-4])[\\)\\.\\]][\\s]+(.*)$"
    );

    private static final Pattern INLINE_OPTION_PATTERN = Pattern.compile(
            "[\\(\\[]([A-Da-d1-4])[\\)\\]][\\s]+([^\\(\\)\\[\\]\\n]+)"
    );

    private static final Pattern ANSWER_KEY_PATTERN = Pattern.compile(
            "(?i)(?:ans(?:wer)?|key)\\s*[:\\-\\s]+[\\(\\[]?([A-Da-d1-4])[\\)\\]]?"
    );

    private static final Pattern DEVANAGARI_PATTERN = Pattern.compile(
            "[\\u0900-\\u097F]"
    );

    private static final Pattern PAPER_CODE_PATTERN = Pattern.compile(
            "(?i)(?:paper\\s*code|test\\s*booklet\\s*(?:code|series)|code\\s*no\\.?|series)\\s*[:\\-\\s]+([A-Za-z0-9\\-]+)"
    );

    private static final Pattern BOOKLET_SERIAL_PATTERN = Pattern.compile(
            "(?i)(?:test\\s*booklet\\s*no\\.?|booklet\\s*(?:serial\\s*)?no\\.?|sl\\.?\\s*no\\.?|serial\\s*no\\.?)\\s*[:\\-\\s]+([A-Za-z0-9\\-]+)"
    );

    private static final Pattern EXAM_YEAR_PATTERN = Pattern.compile(
            "\\b(20[1-3][0-9])\\b"
    );

    private static final Pattern TOTAL_QUESTIONS_PATTERN = Pattern.compile(
            "(?i)(?:total\\s*(?:no\\.?\\s*of\\s*)?questions|no\\.?\\s*of\\s*questions)\\s*[:\\-\\s]+(\\d+)"
    );

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?i)(?:time\\s*allowed|duration)\\s*[:\\-\\s]+(\\d+(?:\\.\\d+)?)\\s*(hours?|hrs?|minutes?|mins?|h)"
    );

    private static final Pattern MAX_MARKS_PATTERN = Pattern.compile(
            "(?i)(?:maximum\\s*marks|max\\.?\\s*marks|total\\s*marks)\\s*[:\\-\\s]+(\\d+(?:\\.\\d+)?)"
    );

    private static final Pattern NEGATIVE_MARKING_PATTERN = Pattern.compile(
            "(?i)(?:negative\\s*marking|penalty).*?\\b(0\\.\\d+|1/[234]|\\d+%)"
    );

    private static final Pattern SUBJECT_NAME_PATTERN = Pattern.compile(
            "(?i)(?:subject|paper)\\s*[:\\-\\s]+([A-Za-z\\s&]{3,40})(?=\\n|$)"
    );

    private final ChatClient chatClient;
    private final ModelRouter modelRouter;
    private final QuestionPaperPdfParser pdfParser;
    private final ObjectMapper objectMapper;
    private final DeterministicJsonRepairEngine jsonRepairEngine;

    @Value("${app.extraction.deterministic-fallback-enabled:true}")
    private boolean deterministicFallbackEnabled = true;

    public LiteLlmPageExtractionClient(
            @Autowired(required = false) ChatClient chatClient,
            ModelRouter modelRouter,
            QuestionPaperPdfParser pdfParser,
            ObjectMapper objectMapper,
            @Autowired(required = false) DeterministicJsonRepairEngine jsonRepairEngine) {
        this.chatClient = chatClient;
        this.modelRouter = modelRouter;
        this.pdfParser = pdfParser;
        this.objectMapper = objectMapper;
        this.jsonRepairEngine = jsonRepairEngine != null ? jsonRepairEngine : new DeterministicJsonRepairEngine();
    }

    /**
     * Extracts cover metadata or questions from a parsed PDF page.
     * Invokes LiteLLM when available, falling back to deterministic heuristic parsing
     * if LiteLLM is offline or disabled.
     *
     * @param pageContent parsed page containing text, image streams, and tier
     * @param examContextHint optional examination context hint (e.g. "JEE Advanced Physics Paper 1")
     * @return structured {@link LlmPageExtractionResult}
     */
    public LlmPageExtractionResult extractPage(PdfPageContent pageContent, String examContextHint) {
        if (pageContent == null) {
            return new LlmPageExtractionResult();
        }

        boolean requiresVision = pageContent.isScannedOrImageHeavy()
                || (pageContent.getEmbeddedDiagramDataUris() != null && !pageContent.getEmbeddedDiagramDataUris().isEmpty());
        String modelName = modelRouter.selectPaperExtractionModel(requiresVision);

        if (chatClient != null) {
            try {
                log.info("Extracting page {}/{} using LiteLLM model '{}' (tier={}, requiresVision={})",
                        pageContent.getPageNumber(), pageContent.getTotalPages(), modelName, pageContent.getParsingTier(), requiresVision);

                String systemPrompt = buildSystemPrompt();
                String userPromptText = buildUserPrompt(pageContent, examContextHint);

                String rawResponse = chatClient.prompt()
                        .system(systemPrompt)
                        .user(u -> {
                            u.text(userPromptText);
                            attachMediaIfPresent(u, pageContent);
                        })
                        .options(ChatOptions.builder().model(modelName).temperature(0.1))
                        .call()
                        .content();

                return parseAndNormalizeLlmResponse(rawResponse, pageContent);
            } catch (Exception e) {
                log.warn("LiteLLM page extraction failed for page {}: {}. Evaluating fallback...",
                        pageContent.getPageNumber(), e.getMessage());
                if (!deterministicFallbackEnabled) {
                    throw new RuntimeException("LiteLLM extraction failed and deterministic fallback is disabled", e);
                }
            }
        } else {
            log.info("ChatClient is not configured. Using deterministic extraction for page {}", pageContent.getPageNumber());
        }

        return extractDeterministically(pageContent);
    }

    /**
     * Local deterministic extraction engine that parses question stems, options, formulas,
     * and cover page metadata using robust heuristics and regex state matching.
     *
     * @param pageContent parsed page content
     * @return structured {@link LlmPageExtractionResult}
     */
    public LlmPageExtractionResult extractDeterministically(PdfPageContent pageContent) {
        if (pageContent == null) {
            return new LlmPageExtractionResult();
        }

        String rawText = pageContent.getDigitalText();
        String text = rawText != null ? rawText.trim() : "";
        int pageNumber = pageContent.getPageNumber();

        LlmPageExtractionResult.LlmPageExtractionResultBuilder resultBuilder = LlmPageExtractionResult.builder();
        List<LlmPageExtractionResult.ExtractedItemPayload> questions = new ArrayList<>();

        // Check if this page is a Cover / Instruction page
        boolean isCover = isCoverPageCandidate(text, pageNumber);
        resultBuilder.coverPage(isCover);

        if (isCover) {
            populateCoverMetadata(resultBuilder, text);
            log.info("Page {} deterministically identified as Cover Page", pageNumber);
        }

        // Segment questions
        List<QuestionSegment> segments = findQuestionSegments(text);
        if (!segments.isEmpty()) {
            for (QuestionSegment segment : segments) {
                LlmPageExtractionResult.ExtractedItemPayload item = parseQuestionSegment(segment, pageContent);
                if (item != null) {
                    questions.add(item);
                }
            }
        } else if (!isCover && text.length() >= QuestionPaperPdfParser.MIN_TEXT_LENGTH_THRESHOLD) {
            // No question numbers detected, but text is substantial and not a cover page
            LlmPageExtractionResult.ExtractedItemPayload fallbackItem = parseSingleFallbackQuestion(text, pageContent);
            if (fallbackItem != null) {
                questions.add(fallbackItem);
            }
        } else if (pageContent.isScannedOrImageHeavy()) {
            // Scanned page without readable text
            questions.add(LlmPageExtractionResult.ExtractedItemPayload.builder()
                    .questionNumber(pageNumber)
                    .questionType(QuestionTypeConstants.SINGLE_MCQ)
                    .content("Scanned Question Page " + pageNumber)
                    .hasDiagram(true)
                    .suggestedDifficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                    .suggestedCognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.APPLY)
                    .confidence(0.60)
                    .build());
        }

        resultBuilder.questions(questions);
        return resultBuilder.build();
    }

    private void attachMediaIfPresent(org.springframework.ai.chat.client.ChatClient.PromptUserSpec userSpec, PdfPageContent pageContent) {
        if (pageContent.isScannedOrImageHeavy() && pageContent.getRenderedPageDataUri() != null) {
            byte[] bytes = decodeDataUri(pageContent.getRenderedPageDataUri());
            if (bytes != null && bytes.length > 0) {
                userSpec.media(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(bytes)));
            }
        }
        if (pageContent.getEmbeddedDiagramDataUris() != null) {
            int maxDiagrams = Math.min(3, pageContent.getEmbeddedDiagramDataUris().size());
            for (int i = 0; i < maxDiagrams; i++) {
                byte[] bytes = decodeDataUri(pageContent.getEmbeddedDiagramDataUris().get(i));
                if (bytes != null && bytes.length > 0) {
                    userSpec.media(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(bytes)));
                }
            }
        }
    }

    private byte[] decodeDataUri(String dataUri) {
        if (dataUri == null || !dataUri.contains(",")) {
            return null;
        }
        try {
            String base64Part = dataUri.substring(dataUri.indexOf(',') + 1);
            return Base64.getDecoder().decode(base64Part.trim());
        } catch (Exception e) {
            log.warn("Failed to decode base64 data URI: {}", e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt() {
        return """
            You are an expert Question Paper Digitization and Extraction AI specializing in high-stakes national examinations (e.g. UPSC, State PSC, SSC, JEE, NEET, GATE).
            Your task is to analyze the provided Question Paper page (visual page scan and/or digital text) and extract all content into a strictly conformant JSON object.

            ### CORE EXTRACTION RULES:
            1. COVER & INSTRUCTION PAGE DETECTION:
               - If the page contains examination instructions, booklet headers, candidate warnings, or paper metadata (common on Page 1):
                 Set "coverPage": true and extract "detectedPaperCode", "detectedBookletSerialNo", "detectedExamYear", "detectedSubjectName", "detectedTotalQuestions", "detectedDurationMinutes", "detectedMaxMarks", "detectedNegativeMarkingRatio", "detectedSections".
               - If this page is an ordinary question page, set "coverPage": false.

            2. BILINGUAL SCANNED QUESTION PAPERS (PARALLEL TWO-COLUMN LAYOUT):
               - National and state competitive exam papers typically feature a parallel two-column layout:
                 * LEFT COLUMN: Question text and options in ENGLISH.
                 * RIGHT COLUMN: Identical question text and options in HINDI (Devanagari script).
               - DO NOT create two separate question items for the same question number!
               - Pair each question by its printed question number:
                 * Map the English stem to "content".
                 * Map English choices to "options": [{"id": "A", "text": "...", "isCorrect": false}, ...].
                 * Map the Hindi stem (in Devanagari script) to "hindiContent".
                 * Map Hindi choices to "hindiOptions": [{"id": "A", "text": "...", "isCorrect": false}, ...].
                 * Ensure option IDs ("A", "B", "C", "D") match between English and Hindi, even if Hindi options are numbered (1), (2), (3), (4) or (क), (ख), (ग), (घ).

            3. QUESTION EXTRACTION & TYPING:
               - Extract every distinct question item into the "questions" array in ascending numerical order.
               - Extract printed question number into "questionNumber".
               - Identify question type: SINGLE_MCQ, MULTI_MCQ, MATRIX_MATCH, NUMERICAL, DESCRIPTIVE, ASSERTION_REASON, PARAGRAPH_SET.
               - If the correct answer key is marked or evident, populate "answerKey" (e.g. "B") and mark the matching option "isCorrect": true.

            4. STRICT LATEX MATH & SCIENCE FORMULAS ($$...$$ ONLY):
               - Enclose ALL mathematical, physical, and chemical formulas, variables, Greek letters, fractions, superscripts, subscripts, and physical units in double dollars: $$ ... $$.
               - STRICTLY FORBIDDEN: Single dollar ($ ... $), \\( ... \\), and \\[ ... \\].
               - Chemistry notation: Use \\ce{...} inside $$...$$ (e.g., $$\\ce{H2SO4}$$).
               - Physical units: Use \\pu{...} inside $$...$$ (e.g., $$\\pu{9.8 m/s^2}$$).
               - Escape backslashes properly in JSON strings (e.g. "$$\\frac{a}{b}$$", "$$\\sqrt{x}$$").

            5. MARKDOWN & TABLE FORMATTING:
               - Multi-statement reasoning: Format statements as clean numbered or Roman lists with double line breaks:
                 English: "**Statements:**\\n1. Statement one.\\n2. Statement two.\\n\\n**Conclusions:**\\nI. Conclusion one.\\nII. Conclusion two."
                 Hindi: "**कथन:**\\n1. पहला कथन।\\n2. दूसरा कथन।\\n\\n**निष्कर्ष:**\\nI. पहला निष्कर्ष।\\nII. दूसरा निष्कर्ष।"
               - Match-the-following questions (List-I / List-II or सूची-I / सूची-II): Format as clean GitHub-Flavored Markdown (GFM) pipe tables in both "content" and "hindiContent".

            6. OUTPUT FORMAT:
               - Output ONLY a single JSON object conforming to the schema. No conversational preambles, notes, or markdown outside of ```json ... ``` code fence.
            """;
    }

    private String buildUserPrompt(PdfPageContent pageContent, String examContextHint) {
        StringBuilder sb = new StringBuilder();
        if (examContextHint != null && !examContextHint.isBlank()) {
            sb.append("Exam Context Hint: ").append(examContextHint.trim()).append("\n\n");
        }
        sb.append("Page ").append(pageContent.getPageNumber())
          .append(" of ").append(pageContent.getTotalPages()).append("\n");
        sb.append("Extraction Tier: ").append(pageContent.getParsingTier()).append("\n");
        if (pageContent.isScannedOrImageHeavy()) {
            sb.append("Note: This page is a scanned document page image (attached). Transcribe all visual text carefully.\n");
            sb.append("Notice: If parallel English (left) and Hindi (right) columns are present, align them into content/options and hindiContent/hindiOptions.\n");
        }
        if (pageContent.getEmbeddedDiagramDataUris() != null && !pageContent.getEmbeddedDiagramDataUris().isEmpty()) {
            sb.append("Note: ").append(pageContent.getEmbeddedDiagramDataUris().size())
              .append(" embedded raster diagram(s) extracted from this page.\n");
        }
        sb.append("\n--- EXTRACTED DIGITAL TEXT STREAM ---\n");
        sb.append(pageContent.getDigitalText() != null ? pageContent.getDigitalText() : "(No digital text detected - rely on visual page image)");
        sb.append("\n--- END DIGITAL TEXT ---\n\n");
        sb.append("Extract all cover metadata (if page 1 / instruction page) and all question items into structured JSON.");
        return sb.toString();
    }

    private LlmPageExtractionResult parseAndNormalizeLlmResponse(String rawResponse, PdfPageContent pageContent) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Empty response received from LiteLLM");
        }

        // Step 1: Strip markdown code fences
        String json = stripCodeFences(rawResponse);

        // Step 2: Clean template brace artifacts and sanitize LaTeX control character collisions
        json = jsonRepairEngine.cleanStringTemplateBraces(json);
        json = DeterministicJsonRepairEngine.sanitizeLatexJson(json);
        json = jsonRepairEngine.repairTruncatedJson(json);

        // Step 3: Parse JSON into result model
        LlmPageExtractionResult result = objectMapper.readValue(json, LlmPageExtractionResult.class);

        // Step 4: Normalize LaTeX formatting on questions
        if (result.getQuestions() != null) {
            for (LlmPageExtractionResult.ExtractedItemPayload q : result.getQuestions()) {
                if (q.getContent() != null) {
                    q.setContent(pdfParser.normalizeMathAndFormatting(q.getContent()));
                }
                if (q.getHindiContent() != null) {
                    q.setHindiContent(pdfParser.normalizeMathAndFormatting(q.getHindiContent()));
                }
                if (q.getOptions() != null) {
                    for (QuestionOption opt : q.getOptions()) {
                        if (opt.getText() != null) {
                            opt.setText(pdfParser.normalizeMathAndFormatting(opt.getText()));
                        }
                    }
                }
                if (q.getHindiOptions() != null) {
                    for (QuestionOption opt : q.getHindiOptions()) {
                        if (opt.getText() != null) {
                            opt.setText(pdfParser.normalizeMathAndFormatting(opt.getText()));
                        }
                    }
                }
                if (pageContent.getEmbeddedDiagramDataUris() != null && !pageContent.getEmbeddedDiagramDataUris().isEmpty()) {
                    q.setHasDiagram(true);
                }
            }
        }

        return result;
    }

    private String stripCodeFences(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        Matcher matcher = CODE_FENCE_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return jsonRepairEngine.stripMarkdownAndPreambles(trimmed);
    }

    private boolean isCoverPageCandidate(String text, int pageNumber) {
        if (text == null || text.isBlank()) {
            return pageNumber == 1;
        }
        String lower = text.toLowerCase();
        int indicators = 0;
        if (lower.contains("instruction") || lower.contains("निर्देश")) indicators++;
        if (lower.contains("candidate") || lower.contains("परीक्षार्थी")) indicators++;
        if (lower.contains("booklet") || lower.contains("पुस्तिका")) indicators++;
        if (lower.contains("do not open") || lower.contains("जब तक न कहा जाए")) indicators++;
        if (lower.contains("time allowed") || lower.contains("समय")) indicators++;
        if (lower.contains("maximum marks") || lower.contains("पूर्णांक")) indicators++;
        if (lower.contains("negative mark") || lower.contains("ऋणात्मक")) indicators++;

        return (pageNumber == 1 && indicators >= 2) || indicators >= 4;
    }

    private void populateCoverMetadata(LlmPageExtractionResult.LlmPageExtractionResultBuilder builder, String text) {
        String paperCode = extractRegex(PAPER_CODE_PATTERN, text, 1);
        String bookletSerial = extractRegex(BOOKLET_SERIAL_PATTERN, text, 1);
        String yearStr = extractRegex(EXAM_YEAR_PATTERN, text, 1);
        String totalQStr = extractRegex(TOTAL_QUESTIONS_PATTERN, text, 1);
        String maxMarksStr = extractRegex(MAX_MARKS_PATTERN, text, 1);
        String subject = extractRegex(SUBJECT_NAME_PATTERN, text, 1);

        builder.detectedPaperCode(paperCode);
        builder.detectedBookletSerialNo(bookletSerial);
        builder.detectedSubjectName(subject);

        if (yearStr != null) {
            try {
                builder.detectedExamYear(Integer.parseInt(yearStr));
            } catch (NumberFormatException ignored) {}
        }
        if (totalQStr != null) {
            try {
                builder.detectedTotalQuestions(Integer.parseInt(totalQStr));
            } catch (NumberFormatException ignored) {}
        }
        if (maxMarksStr != null) {
            try {
                builder.detectedMaxMarks(Double.parseDouble(maxMarksStr));
            } catch (NumberFormatException ignored) {}
        }

        // Duration detection (handles hours or minutes)
        Matcher durationMatcher = DURATION_PATTERN.matcher(text);
        if (durationMatcher.find()) {
            try {
                double val = Double.parseDouble(durationMatcher.group(1));
                String unit = durationMatcher.group(2).toLowerCase();
                if (unit.startsWith("h")) {
                    builder.detectedDurationMinutes((int) Math.round(val * 60));
                } else {
                    builder.detectedDurationMinutes((int) Math.round(val));
                }
            } catch (Exception ignored) {}
        }

        // Negative marking ratio detection
        Matcher negMatcher = NEGATIVE_MARKING_PATTERN.matcher(text);
        if (negMatcher.find()) {
            String ratioToken = negMatcher.group(1).trim();
            builder.detectedNegativeMarkingRatio(parseNegativeMarkingRatio(ratioToken));
        }
    }

    private Double parseNegativeMarkingRatio(String token) {
        try {
            if (token.endsWith("%")) {
                return Double.parseDouble(token.replace("%", "").trim()) / 100.0;
            }
            if (token.contains("/")) {
                String[] parts = token.split("/");
                return Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
            }
            return Double.parseDouble(token);
        } catch (Exception e) {
            return 0.25;
        }
    }

    private List<QuestionSegment> findQuestionSegments(String text) {
        List<QuestionSegment> segments = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return segments;
        }

        Matcher matcher = QUESTION_HEADER_PATTERN.matcher(text);
        List<int[]> matchPositions = new ArrayList<>();
        List<Integer> questionNumbers = new ArrayList<>();

        while (matcher.find()) {
            matchPositions.add(new int[]{matcher.start(), matcher.end()});
            try {
                questionNumbers.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                questionNumbers.add(matchPositions.size());
            }
        }

        for (int i = 0; i < matchPositions.size(); i++) {
            int start = matchPositions.get(i)[0];
            int end = (i + 1 < matchPositions.size()) ? matchPositions.get(i + 1)[0] : text.length();
            int qNum = questionNumbers.get(i);
            String chunk = text.substring(start, end).trim();
            segments.add(new QuestionSegment(qNum, chunk));
        }

        return segments;
    }

    private LlmPageExtractionResult.ExtractedItemPayload parseQuestionSegment(QuestionSegment segment, PdfPageContent pageContent) {
        String chunk = segment.rawText();
        int questionNumber = segment.questionNumber();

        // Strip leading "1. " or "Q.1 "
        Matcher headerMatcher = QUESTION_HEADER_PATTERN.matcher(chunk);
        String body = headerMatcher.find() ? chunk.substring(headerMatcher.end()).trim() : chunk;

        List<QuestionOption> options = extractOptions(body);
        String stemText = extractStem(body, options);

        // Normalize math on stem
        String normalizedStem = pdfParser.normalizeMathAndFormatting(stemText);

        // Detect Hindi translations
        String primaryContent = normalizedStem;
        String hindiContent = null;
        if (DEVANAGARI_PATTERN.matcher(normalizedStem).find()) {
            String[] split = splitBilingualText(normalizedStem);
            if (split[0] != null && !split[0].isBlank() && split[1] != null && !split[1].isBlank()) {
                primaryContent = split[0];
                hindiContent = split[1];
            }
        }

        // Detect answer key if printed in segment
        String answerKey = null;
        Matcher keyMatcher = ANSWER_KEY_PATTERN.matcher(chunk);
        if (keyMatcher.find()) {
            answerKey = normalizeOptionId(keyMatcher.group(1));
            for (QuestionOption opt : options) {
                if (opt.getId().equalsIgnoreCase(answerKey)) {
                    opt.setCorrect(true);
                }
            }
        }

        boolean hasFormulas = primaryContent.contains("$$") || primaryContent.contains("=");
        boolean hasTables = primaryContent.contains("|") && primaryContent.contains("---");
        boolean hasDiagram = pageContent.getEmbeddedDiagramDataUris() != null && !pageContent.getEmbeddedDiagramDataUris().isEmpty();

        String questionType;
        if (options.size() >= 2) {
            questionType = QuestionTypeConstants.SINGLE_MCQ;
        } else if (primaryContent.toUpperCase().contains("MATRIX") || (primaryContent.contains("List-I") && primaryContent.contains("List-II"))) {
            questionType = QuestionTypeConstants.MATRIX_MATCH;
        } else if (primaryContent.toUpperCase().contains("ASSERTION") && primaryContent.toUpperCase().contains("REASON")) {
            questionType = QuestionTypeConstants.ASSERTION_REASON;
        } else if (hasFormulas && options.isEmpty()) {
            questionType = QuestionTypeConstants.NUMERICAL;
        } else {
            questionType = QuestionTypeConstants.DESCRIPTIVE;
        }

        double confidence = options.size() >= 4 ? 0.95 : (options.size() >= 2 ? 0.85 : 0.80);

        return LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(questionNumber)
                .questionType(questionType)
                .content(primaryContent)
                .options(options)
                .answerKey(answerKey)
                .hindiContent(hindiContent)
                .hasFormulas(hasFormulas)
                .hasTables(hasTables)
                .hasDiagram(hasDiagram)
                .suggestedDifficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .suggestedCognitiveLevel(hasFormulas ? QuestionTaxonomyConstants.CognitiveLevel.ANALYZE : QuestionTaxonomyConstants.CognitiveLevel.UNDERSTAND)
                .confidence(confidence)
                .build();
    }

    private LlmPageExtractionResult.ExtractedItemPayload parseSingleFallbackQuestion(String text, PdfPageContent pageContent) {
        List<QuestionOption> options = extractOptions(text);
        String stem = extractStem(text, options);
        String normalizedStem = pdfParser.normalizeMathAndFormatting(stem);

        boolean hasFormulas = normalizedStem.contains("$$") || normalizedStem.contains("=");
        boolean hasDiagram = pageContent.getEmbeddedDiagramDataUris() != null && !pageContent.getEmbeddedDiagramDataUris().isEmpty();

        return LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(pageContent.getPageNumber())
                .questionType(options.size() >= 2 ? QuestionTypeConstants.SINGLE_MCQ : QuestionTypeConstants.DESCRIPTIVE)
                .content(normalizedStem)
                .options(options)
                .hasFormulas(hasFormulas)
                .hasDiagram(hasDiagram)
                .suggestedDifficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .suggestedCognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.UNDERSTAND)
                .confidence(0.70)
                .build();
    }

    private List<QuestionOption> extractOptions(String text) {
        List<QuestionOption> options = new ArrayList<>();
        if (text == null) return options;

        // Try multiline options first
        Matcher lineMatcher = OPTION_LINE_PATTERN.matcher(text);
        List<int[]> lineMatches = new ArrayList<>();
        List<String> optionIds = new ArrayList<>();

        while (lineMatcher.find()) {
            lineMatches.add(new int[]{lineMatcher.start(), lineMatcher.end()});
            optionIds.add(normalizeOptionId(lineMatcher.group(1)));
        }

        if (lineMatches.size() >= 2) {
            for (int i = 0; i < lineMatches.size(); i++) {
                int contentStart = lineMatches.get(i)[1];
                int contentEnd = (i + 1 < lineMatches.size()) ? lineMatches.get(i + 1)[0] : text.length();
                String optText = text.substring(contentStart, contentEnd).trim();
                options.add(QuestionOption.builder()
                        .id(optionIds.get(i))
                        .text(pdfParser.normalizeMathAndFormatting(optText))
                        .correct(false)
                        .build());
            }
            return options;
        }

        // Try inline options e.g. (A) 10 (B) 20 (C) 30 (D) 40
        Matcher inlineMatcher = INLINE_OPTION_PATTERN.matcher(text);
        while (inlineMatcher.find()) {
            String id = normalizeOptionId(inlineMatcher.group(1));
            String optText = inlineMatcher.group(2).trim();
            options.add(QuestionOption.builder()
                    .id(id)
                    .text(pdfParser.normalizeMathAndFormatting(optText))
                    .correct(false)
                    .build());
        }

        return options;
    }

    private String extractStem(String text, List<QuestionOption> options) {
        if (options.isEmpty()) {
            return text.trim();
        }
        Matcher lineMatcher = OPTION_LINE_PATTERN.matcher(text);
        if (lineMatcher.find()) {
            return text.substring(0, lineMatcher.start()).trim();
        }
        Matcher inlineMatcher = INLINE_OPTION_PATTERN.matcher(text);
        if (inlineMatcher.find()) {
            return text.substring(0, inlineMatcher.start()).trim();
        }
        return text.trim();
    }

    private String[] splitBilingualText(String text) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder english = new StringBuilder();
        StringBuilder hindi = new StringBuilder();

        for (String line : lines) {
            if (DEVANAGARI_PATTERN.matcher(line).find()) {
                hindi.append(line).append("\n");
            } else {
                english.append(line).append("\n");
            }
        }

        return new String[]{english.toString().trim(), hindi.toString().trim()};
    }

    private String normalizeOptionId(String token) {
        if (token == null) return "A";
        String trimmed = token.trim();
        return switch (trimmed.toUpperCase()) {
            case "1", "A" -> "A";
            case "2", "B" -> "B";
            case "3", "C" -> "C";
            case "4", "D" -> "D";
            default -> trimmed.toUpperCase();
        };
    }

    private String extractRegex(Pattern pattern, String text, int group) {
        if (text == null) return null;
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group).trim();
        }
        return null;
    }

    private record QuestionSegment(int questionNumber, String rawText) {}
}
