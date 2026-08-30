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

package com.examplatform.questionbank.io;

import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.service.QuestionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports questions from a compressed ZIP archive produced by
 * {@link QuestionExportService} (or a compatible tool).
 *
 * <p>The archive may contain several batch files (JSON arrays or CSV) plus an
 * optional {@code manifest.json}. Each record is converted to a
 * {@link CreateQuestionRequest} and created through the same
 * {@link QuestionService#createQuestion} path used by the API, so validation,
 * hierarchy resolution, encryption, embedding, and audit all apply uniformly.
 * A record that fails validation or creation is recorded in the result and does
 * not abort the rest of the import.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportService {

    /** Cap on the number of failures echoed back, to bound response size. */
    private static final int MAX_FAILURES_REPORTED = 200;

    private final QuestionService questionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads and imports every batch file in the archive.
     *
     * @param zipBytes  raw bytes of the uploaded ZIP archive
     * @param authorId  the importing user (becomes the question author)
     * @param tenantId  tenant identifier
     */
    public ImportResult importFromZip(byte[] zipBytes, UUID authorId, String tenantId) throws IOException {
        ImportResult result = ImportResult.builder().build();

        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || name.endsWith("manifest.json")) {
                    zip.closeEntry();
                    continue;
                }
                String lower = name.toLowerCase();
                boolean json = lower.endsWith(".json");
                boolean csv = lower.endsWith(".csv");
                if (!json && !csv) {
                    zip.closeEntry();
                    continue;
                }

                String content = readEntry(zip);
                zip.closeEntry();

                result.setFilesProcessed(result.getFilesProcessed() + 1);
                if (json) {
                    importJsonFile(name, content, authorId, tenantId, result);
                } else {
                    importCsvFile(name, content, authorId, tenantId, result);
                }
            }
        }

        log.info("Import complete: files={}, records={}, imported={}, failed={}, tenant={}",
                result.getFilesProcessed(), result.getTotalRecords(),
                result.getImported(), result.getFailed(), tenantId);
        return result;
    }

    private void importJsonFile(String file, String content, UUID authorId,
                                String tenantId, ImportResult result) {
        List<Map<String, Object>> records;
        try {
            records = objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            // whole-file parse failure — record one failure for the file
            recordFailure(result, file, -1, "Failed to parse JSON: " + e.getMessage());
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            result.setTotalRecords(result.getTotalRecords() + 1);
            try {
                CreateQuestionRequest request = fromJsonRecord(records.get(i));
                questionService.createQuestion(request, authorId, tenantId);
                result.setImported(result.getImported() + 1);
            } catch (Exception e) {
                recordFailure(result, file, i, e.getMessage());
            }
        }
    }

    private void importCsvFile(String file, String content, UUID authorId,
                               String tenantId, ImportResult result) {
        List<List<String>> rows = CsvUtil.parse(content);
        if (rows.isEmpty()) {
            return;
        }
        List<String> header = rows.get(0);
        for (int r = 1; r < rows.size(); r++) {
            result.setTotalRecords(result.getTotalRecords() + 1);
            try {
                CreateQuestionRequest request = fromCsvRow(header, rows.get(r));
                questionService.createQuestion(request, authorId, tenantId);
                result.setImported(result.getImported() + 1);
            } catch (Exception e) {
                recordFailure(result, file, r - 1, e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Record -> CreateQuestionRequest mapping
    // -----------------------------------------------------------------------

    private CreateQuestionRequest fromJsonRecord(Map<String, Object> rec) {
        List<QuestionOption> options = null;
        Object rawOptions = rec.get("options");
        if (rawOptions != null) {
            options = objectMapper.convertValue(rawOptions, new TypeReference<List<QuestionOption>>() {});
        }
        return CreateQuestionRequest.builder()
                .subjectId(asLong(rec.get("subjectId")))
                .topicId(asLong(rec.get("topicId")))
                .subtopicId(asLong(rec.get("subtopicId")))
                .subject(asString(rec.get("subject")))
                .topic(asString(rec.get("topic")))
                .subtopic(asString(rec.get("subtopic")))
                .chapter(asString(rec.get("chapter")))
                .difficulty(parseDifficulty(asString(rec.get("difficulty"))))
                .cognitiveLevel(parseCognitive(asString(rec.get("cognitiveLevel"))))
                .questionType(parseType(asString(rec.get("questionType"))))
                .content(asString(rec.get("content")))
                .answerKey(asString(rec.get("answerKey")))
                .explanation(asString(rec.get("explanation")))
                .references(asString(rec.get("references")))
                .options(options)
                .build();
    }

    private CreateQuestionRequest fromCsvRow(List<String> header, List<String> row) throws IOException {
        String optionsJson = value(header, row, "optionsJson");
        List<QuestionOption> options = null;
        if (optionsJson != null && !optionsJson.isBlank()) {
            options = objectMapper.readValue(optionsJson, new TypeReference<List<QuestionOption>>() {});
        }
        return CreateQuestionRequest.builder()
                .subjectId(asLong(value(header, row, "subjectId")))
                .topicId(asLong(value(header, row, "topicId")))
                .subtopicId(asLong(value(header, row, "subtopicId")))
                .subject(value(header, row, "subject"))
                .topic(value(header, row, "topic"))
                .subtopic(value(header, row, "subtopic"))
                .chapter(value(header, row, "chapter"))
                .difficulty(parseDifficulty(value(header, row, "difficulty")))
                .cognitiveLevel(parseCognitive(value(header, row, "cognitiveLevel")))
                .questionType(parseType(value(header, row, "questionType")))
                .content(value(header, row, "content"))
                .answerKey(value(header, row, "answerKey"))
                .explanation(value(header, row, "explanation"))
                .references(value(header, row, "references"))
                .options(options)
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String readEntry(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = zip.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String value(List<String> header, List<String> row, String column) {
        int idx = header.indexOf(column);
        if (idx < 0 || idx >= row.size()) {
            return null;
        }
        String v = row.get(idx);
        return (v == null || v.isEmpty()) ? null : v;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        return Long.parseLong(s);
    }

    private static DifficultyLevel parseDifficulty(String v) {
        return v == null || v.isBlank() ? null : DifficultyLevel.valueOf(v.trim().toUpperCase());
    }

    private static CognitiveLevel parseCognitive(String v) {
        return v == null || v.isBlank() ? null : CognitiveLevel.valueOf(v.trim().toUpperCase());
    }

    private static QuestionType parseType(String v) {
        return v == null || v.isBlank() ? null : QuestionType.valueOf(v.trim().toUpperCase());
    }

    private void recordFailure(ImportResult result, String file, int recordIndex, String error) {
        result.setFailed(result.getFailed() + 1);
        if (result.getFailures().size() < MAX_FAILURES_REPORTED) {
            result.getFailures().add(ImportResult.FailedRecord.builder()
                    .file(file)
                    .recordIndex(recordIndex)
                    .error(error != null ? error : "Unknown error")
                    .build());
        }
    }
}
