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

import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports questions to a compressed ZIP archive.
 *
 * <p>Questions matching the given filters are streamed in pages, grouped into
 * batch files of {@link #BATCH_SIZE} questions each. For a large result set this
 * produces several batch files inside a single ZIP, plus a {@code manifest.json}
 * describing the export. Two on-disk formats are supported: {@code json} (one
 * JSON array per batch file) and {@code csv} (one header + rows per batch file).
 *
 * <p>The archive is written directly to the supplied {@link OutputStream}
 * (typically the HTTP response) so memory stays bounded to a single page/batch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionExportService {

    /** Number of questions per batch file, per requirements. */
    public static final int BATCH_SIZE = 100;

    /** CSV column order used for both export and import. */
    static final List<String> CSV_HEADERS = List.of(
            "id", "subjectId", "topicId", "subtopicId",
            "subject", "topic", "subtopic", "chapter",
            "difficulty", "cognitiveLevel", "questionType",
            "content", "answerKey", "explanation", "references",
            "state", "optionsJson");

    private final QuestionService questionService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Streams a ZIP export of questions to {@code out}.
     *
     * @param format    "json" or "csv" (case-insensitive); defaults to json
     * @param subject   optional subject-name filter
     * @param topic     optional topic-name filter
     * @param difficulty optional difficulty filter
     * @param state     optional lifecycle-state filter
     * @param search    optional free-text filter
     * @param tenantId  tenant identifier
     * @return number of questions exported
     */
    public int exportToZip(String format, String subject, String topic, String difficulty,
                           String state, String search, String tenantId, OutputStream out) throws IOException {
        boolean csv = "csv".equalsIgnoreCase(format);
        int total = 0;
        int batchNumber = 0;
        int pageNumber = 0;
        List<QuestionResponse> currentBatch = new ArrayList<>(BATCH_SIZE);
        List<String> batchFileNames = new ArrayList<>();

        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            Page<QuestionResponse> page;
            do {
                page = questionService.listQuestions(
                        subject, topic, difficulty, state, search, pageNumber, BATCH_SIZE, tenantId);
                for (QuestionResponse q : page.getContent()) {
                    currentBatch.add(q);
                    total++;
                    if (currentBatch.size() == BATCH_SIZE) {
                        batchNumber++;
                        batchFileNames.add(writeBatch(zip, currentBatch, batchNumber, csv));
                        currentBatch.clear();
                    }
                }
                pageNumber++;
            } while (page.hasNext());

            // flush trailing partial batch
            if (!currentBatch.isEmpty()) {
                batchNumber++;
                batchFileNames.add(writeBatch(zip, currentBatch, batchNumber, csv));
                currentBatch.clear();
            }

            writeManifest(zip, csv ? "csv" : "json", total, batchFileNames, tenantId);
        }

        log.info("Exported {} questions in {} batch file(s), format={}, tenant={}",
                total, batchNumber, csv ? "csv" : "json", tenantId);
        return total;
    }

    private String writeBatch(ZipOutputStream zip, List<QuestionResponse> batch,
                              int batchNumber, boolean csv) throws IOException {
        String fileName = String.format("questions-batch-%04d.%s", batchNumber, csv ? "csv" : "json");
        zip.putNextEntry(new ZipEntry(fileName));
        if (csv) {
            zip.write(toCsv(batch).getBytes(StandardCharsets.UTF_8));
        } else {
            zip.write(objectMapper.writeValueAsBytes(batch));
        }
        zip.closeEntry();
        return fileName;
    }

    private String toCsv(List<QuestionResponse> batch) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtil.writeRow(CSV_HEADERS));
        for (QuestionResponse q : batch) {
            String optionsJson = q.getOptions() == null ? ""
                    : objectMapper.writeValueAsString(q.getOptions());
            sb.append(CsvUtil.writeRow(List.of(
                    str(q.getId()),
                    str(q.getSubjectId()),
                    str(q.getTopicId()),
                    str(q.getSubtopicId()),
                    str(q.getSubject()),
                    str(q.getTopic()),
                    str(q.getSubtopic()),
                    str(q.getChapter()),
                    str(q.getDifficulty()),
                    str(q.getCognitiveLevel()),
                    str(q.getQuestionType()),
                    str(q.getContent()),
                    str(q.getAnswerKey()),
                    str(q.getExplanation()),
                    str(q.getReferences()),
                    str(q.getState()),
                    optionsJson)));
        }
        return sb.toString();
    }

    private void writeManifest(ZipOutputStream zip, String format, int total,
                               List<String> batchFiles, String tenantId) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", "nag.question-export.v1");
        manifest.put("format", format);
        manifest.put("tenantId", tenantId);
        manifest.put("exportedAt", Instant.now().toString());
        manifest.put("batchSize", BATCH_SIZE);
        manifest.put("totalQuestions", total);
        manifest.put("batchFiles", batchFiles);

        zip.putNextEntry(new ZipEntry("manifest.json"));
        zip.write(objectMapper.writeValueAsBytes(manifest));
        zip.closeEntry();
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
