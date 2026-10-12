/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.service.SimilarityDetectionService;
import com.examplatform.questionbank.service.SubjectTopicService;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.CreateModelInvocationJobResponse;
import software.amazon.awssdk.services.bedrock.model.GetModelInvocationJobResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Service for async batch question generation using AWS Bedrock Batch Inference.
 *
 * <p>Multiple generation items (each with subject/topic/difficulty/type/count) are combined
 * into a single JSONL file, uploaded to S3, and processed as one Bedrock batch job.
 * This minimizes cost by avoiding per-job overhead charges.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockBatchService {

    private static final Set<String> MCQ_TYPES = Set.of(QuestionTypeConstants.SINGLE_MCQ,
        QuestionTypeConstants.MULTI_MCQ);

    private final BatchGenerationJobRepository jobRepository;
    private final QuestionRepository questionRepository;
    private final BedrockClient bedrockClient;
    private final S3Client s3Client;
    private final ModelRouter modelRouter;
    private final EmbeddingService embeddingService;
    private final SimilarityDetectionService similarityDetectionService;
    private final SubjectTopicService subjectTopicService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.bedrock.s3-bucket:exam-bedrock-batch}")
    private String s3Bucket;

    @Value("${app.ai.bedrock.s3-input-prefix:batch-input/}")
    private String s3InputPrefix;

    @Value("${app.ai.bedrock.s3-output-prefix:batch-output/}")
    private String s3OutputPrefix;

    @Value("${app.ai.bedrock.role-arn:}")
    private String bedrockRoleArn;

    /**
     * Submits a batch job containing multiple generation items.
     * All items are packed into one JSONL -> one S3 upload -> one Bedrock job.
     */
    @Transactional
    public BatchJobResponse submitBatchJob(BatchGenerationRequest request, UUID authorId, String tenantId) {
        List<BatchGenerationRequest.BatchItem> itemsList = request.getItems();
        int totalRequested = itemsList.stream().mapToInt(BatchGenerationRequest.BatchItem::getCount).sum();

        String modelId = modelRouter.selectBatchModelId(itemsList.getFirst().getDifficulty());

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(itemsList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize batch items", e);
        }

        BatchGenerationJob job = BatchGenerationJob.builder()
                .items(itemsJson)
                .totalRequested(totalRequested)
                .modelUsed(modelId)
                .avoidDuplicates(request.isAvoidDuplicates())
                .initiatedBy(authorId)
                .status(BatchJobStatus.PENDING)
                .build();
        job.setTenantId(tenantId);
        BatchGenerationJob saved = jobRepository.save(job);

        try {
            String jsonlContent = buildJsonlInput(itemsList);

            String s3Key = s3InputPrefix + saved.getId() + "/input.jsonl";
            s3Client.putObject(
                    b -> b.bucket(s3Bucket).key(s3Key).contentType("application/jsonl"),
                    RequestBody.fromString(jsonlContent, StandardCharsets.UTF_8));

            String s3InputUri = "s3://" + s3Bucket + "/" + s3InputPrefix + saved.getId() + "/";
            String s3OutputUri = "s3://" + s3Bucket + "/" + s3OutputPrefix + saved.getId() + "/";

            CreateModelInvocationJobResponse bedrockResponse = bedrockClient.createModelInvocationJob(
                    b -> b.jobName("exam-batch-" + saved.getId().toString().substring(0, 8))
                            .modelId(modelId)
                            .roleArn(bedrockRoleArn)
                            .inputDataConfig(in -> in.s3InputDataConfig(s3In -> s3In.s3Uri(s3InputUri)))
                            .outputDataConfig(out -> out.s3OutputDataConfig(s3Out -> s3Out.s3Uri(s3OutputUri))));

            saved.setStatus(BatchJobStatus.PROCESSING);
            saved.setStartedAt(Instant.now());
            saved.setBedrockJobArn(bedrockResponse.jobArn());
            jobRepository.save(saved);

            log.info("Bedrock batch job created: id={}, items={}, totalQuestions={}, arn={}",
                    saved.getId(), itemsList.size(), totalRequested, bedrockResponse.jobArn());

        } catch (Exception e) {
            log.error("Failed to submit Bedrock batch job: {}", e.getMessage(), e);
            saved.setStatus(BatchJobStatus.FAILED);
            saved.setErrorMessage("Failed to submit: " + e.getMessage());
            saved.setCompletedAt(Instant.now());
            jobRepository.save(saved);
        }

        return BatchJobResponse.from(saved);
    }

    /**
     * Builds one JSONL file with one record per batch item.
     * Each record has a unique recordId (REC-0000, REC-0001, ...) used to
     * correlate results back to items.
     */
    private String buildJsonlInput(List<BatchGenerationRequest.BatchItem> items) {
        StringBuilder jsonl = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            BatchGenerationRequest.BatchItem item = items.get(i);
            String recordId = "REC-" + String.format("%04d", i);

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(item);
            String modelInput = buildModelInput(systemPrompt, userPrompt);

            try {
                JsonNode inputNode = objectMapper.readTree(modelInput);
                var recordNode = objectMapper.createObjectNode();
                recordNode.put("recordId", recordId);
                recordNode.set("modelInput", inputNode);
                jsonl.append(objectMapper.writeValueAsString(recordNode)).append("\n");
            } catch (Exception _) {
                log.error("Failed to build JSONL record {}", recordId);
            }
        }

        return jsonl.toString();
    }

    private String buildModelInput(String systemPrompt, String userPrompt) {
        return """
                {
                  "messages": [
                    {"role": "user", "content": [{"text": %s}]}
                  ],
                  "system": [{"text": %s}],
                  "inferenceConfig": {
                    "maxTokens": 4096,
                    "temperature": 0.7,
                    "topP": 0.9
                  }
                }
                """.formatted(jsonEscape(userPrompt), jsonEscape(systemPrompt));
    }

    private String buildSystemPrompt() {
        return "You are an expert examination question generator for Indian competitive examinations. "
                + "Generate high-quality questions in structured JSON format. "
                + "Formatting & Syntax Rules: "
                + "- Generate questions strictly matching the specified type, difficulty, and cognitive level. "
                + "- ALL mathematical, physical, and chemical formulas, expressions, variables, percentages, and unit notations in EVERY field (content, options, answerKey, and explanation) MUST be enclosed in $$...$$ LaTeX syntax. "
                + "- NEVER use \\( ... \\) or \\[ ... \\] or single $. "
                + "- In LaTeX math mode ($$...$$), always write percentage symbols as \\% (e.g. $$99.9\\%$$). "
                + "- Use standard Markdown for multi-line formatting (e.g. **Statements:**, **Conclusions:**, tables). "
                + "- Use double newlines (\\n\\n) to separate headings and paragraphs, and single newlines (\\n) between numbered statement items. "
                + "- For MCQ (SINGLE_MCQ): exactly 4 options with ids A, B, C, D. Set isCorrect:true on EXACTLY ONE option and isCorrect:false on the other three. answerKey must be the id (A/B/C/D) of the correct option. "
                + "- For MSQ (MULTI_MCQ): exactly 4 options (A, B, C, D), 2 or more correct. "
                + "- For NUMERICAL: no options, answerKey is the numeric value. "
                + "- For DESCRIPTIVE: no options, answerKey contains the model answer. "
                + "- Always provide a clear, step-by-step explanation. "
                + "- Each question MUST be unique and test a DIFFERENT concept. "
                + "- Use only English language. "
                + "Output ONLY a JSON array of question objects with fields: content, answerKey, explanation, options (for MCQ), difficulty, cognitiveLevel, questionType.";
    }

    private String buildUserPrompt(BatchGenerationRequest.BatchItem item) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate ").append(item.getCount()).append(" UNIQUE question(s):\n");
        prompt.append("- Subject: ").append(item.getSubject()).append("\n");
        prompt.append("- Topic: ").append(item.getTopic()).append("\n");
        if (item.getSubtopic() != null && !item.getSubtopic().isBlank()) {
            prompt.append("- Subtopic: ").append(item.getSubtopic()).append("\n");
        }
        prompt.append("- Difficulty: ").append(item.getDifficulty()).append("\n");
        prompt.append("- Cognitive Level: ").append(item.getCognitiveLevel()).append("\n");
        prompt.append("- Question Type: ").append(item.getQuestionType()).append("\n");
        prompt.append("\nOutput ONLY a JSON array:");
        return prompt.toString();
    }

    private String jsonEscape(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception _) {
            return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void pollProcessingJobs() {
        List<BatchGenerationJob> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(BatchJobStatus.PROCESSING);
        for (BatchGenerationJob job : jobs) {
            try {
                pollJob(job);
            } catch (Exception _) {
                log.error("Error polling batch job {}", job.getId());
            }
        }
    }

    private void pollJob(BatchGenerationJob job) {
        if (job.getBedrockJobArn() == null) {
            job.setStatus(BatchJobStatus.FAILED);
            job.setErrorMessage("No Bedrock job ARN");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            return;
        }

        GetModelInvocationJobResponse resp = bedrockClient.getModelInvocationJob(
                b -> b.jobIdentifier(job.getBedrockJobArn()));

        switch (resp.status()) {
            case COMPLETED -> processCompletedJob(job);
            case FAILED, STOPPED -> {
                job.setStatus(BatchJobStatus.FAILED);
                job.setErrorMessage("Bedrock: " + resp.status() + " - " + resp.message());
                job.setCompletedAt(Instant.now());
                jobRepository.save(job);
            }
            default -> { /* still running */ }
        }
    }

    private static class JobProcessingStats {
        final List<UUID> generatedIds = new ArrayList<>();
        int totalGenerated = 0;
        int totalFailed = 0;
        int totalDuplicates = 0;
    }

    /**
     * Downloads output JSONL, parses each record's model output, validates questions,
     * and persists them. Each output record's recordId maps back to the original item
     * for correct subject/topic metadata.
     */
    private void processCompletedJob(BatchGenerationJob job) {
        log.info("Processing completed batch job: id={}", job.getId());

        Optional<List<BatchGenerationRequest.BatchItem>> itemsOpt = deserializeItems(job);
        if (itemsOpt.isEmpty()) {
            markJobFailed(job, "Failed to deserialize items");
            return;
        }

        List<BatchGenerationRequest.BatchItem> items = itemsOpt.get();
        JobProcessingStats stats = new JobProcessingStats();

        try {
            processS3OutputFiles(job, items, stats);
            finalizeCompletedJob(job, stats);
        } catch (Exception e) {
            log.error("Failed processing batch job {}: {}", job.getId(), e.getMessage(), e);
            markJobFailed(job, "Processing failed: " + e.getMessage());
        }
    }

    private Optional<List<BatchGenerationRequest.BatchItem>> deserializeItems(BatchGenerationJob job) {
        try {
            return Optional.of(objectMapper.readValue(job.getItems(),
                new TypeReference<>() {
                }));
        } catch (Exception _) {
            log.error("Failed to deserialize job items for job {}", job.getId());
            return Optional.empty();
        }
    }

    private void processS3OutputFiles(
            BatchGenerationJob job,
            List<BatchGenerationRequest.BatchItem> items,
            JobProcessingStats stats) {
        String outputPrefix = s3OutputPrefix + job.getId() + "/";
        var listResp = s3Client.listObjectsV2(b -> b.bucket(s3Bucket).prefix(outputPrefix));

        for (S3Object s3Obj : listResp.contents()) {
            if (s3Obj.key().endsWith(".jsonl")) {
                processJsonlFile(s3Obj.key(), job, items, stats);
            }
        }
    }

    private void processJsonlFile(
            String s3Key,
            BatchGenerationJob job,
            List<BatchGenerationRequest.BatchItem> items,
            JobProcessingStats stats) {
        var getResp = s3Client.getObject(b -> b.bucket(s3Bucket).key(s3Key));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResp, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    processOutputLine(line, job, items, stats);
                }
            }
        } catch (Exception _) {
            log.error("Failed to read S3 output file {}", s3Key);
        }
    }

    private void processOutputLine(
            String line,
            BatchGenerationJob job,
            List<BatchGenerationRequest.BatchItem> items,
            JobProcessingStats stats) {
        try {
            JsonNode recordNode = objectMapper.readTree(line);
            String recordId = recordNode.path("recordId").asText("");
            JsonNode modelOutput = recordNode.get("modelOutput");

            if (modelOutput == null || modelOutput.isNull()) {
                stats.totalFailed++;
                return;
            }

            int itemIndex = parseRecordIndex(recordId);
            BatchGenerationRequest.BatchItem item = (itemIndex >= 0 && itemIndex < items.size())
                    ? items.get(itemIndex) : null;

            List<RawQuestion> questions = parseModelOutput(modelOutput);
            for (RawQuestion raw : questions) {
                processRawQuestion(raw, item, job, stats);
            }
        } catch (Exception _) {
            log.warn("Failed to parse output record");
            stats.totalFailed++;
        }
    }

    private void processRawQuestion(
            RawQuestion raw,
            BatchGenerationRequest.BatchItem item,
            BatchGenerationJob job,
            JobProcessingStats stats) {
        String qType = item != null ? item.getQuestionType() : QuestionTypeConstants.SINGLE_MCQ;
        if (!isValid(raw, qType)) {
            stats.totalFailed++;
            return;
        }

        if (job.isAvoidDuplicates() && item != null && isDuplicate(raw.content, item.getSubject(), job.getTenantId())) {
            stats.totalDuplicates++;
            return;
        }

        UUID savedId = persistQuestion(raw, item, job);
        if (savedId != null) {
            stats.generatedIds.add(savedId);
            stats.totalGenerated++;
        } else {
            stats.totalFailed++;
        }
    }

    private void finalizeCompletedJob(BatchGenerationJob job, JobProcessingStats stats) {
        job.setTotalGenerated(stats.totalGenerated);
        job.setTotalFailed(stats.totalFailed);
        job.setTotalDuplicates(stats.totalDuplicates);
        job.setGeneratedQuestionIds(serializeIds(stats.generatedIds));
        job.setStatus(resolveBatchJobStatus(stats.totalGenerated, stats.totalFailed));
        job.setCompletedAt(Instant.now());
        if (stats.totalGenerated == 0) {
            job.setErrorMessage("All records failed validation or duplicate detection");
        }
        jobRepository.save(job);

        log.info("Batch job done: id={}, generated={}, failed={}, duplicates={}",
                job.getId(), stats.totalGenerated, stats.totalFailed, stats.totalDuplicates);
    }

    private void markJobFailed(BatchGenerationJob job, String errorMessage) {
        job.setStatus(BatchJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
    }

    private BatchJobStatus resolveBatchJobStatus(int totalGenerated, int totalFailed) {
        if (totalGenerated <= 0) {
            return BatchJobStatus.FAILED;
        }
        return totalFailed > 0 ? BatchJobStatus.PARTIALLY_COMPLETED : BatchJobStatus.COMPLETED;
    }

    private int parseRecordIndex(String recordId) {
        try {
            return Integer.parseInt(recordId.replace("REC-", ""));
        } catch (Exception _) {
            return -1;
        }
    }

    private UUID persistQuestion(RawQuestion raw, BatchGenerationRequest.BatchItem item, BatchGenerationJob job) {
        try {
            String subjectName = item != null ? item.getSubject() : SharedConstants.UNKNOWN;
            String topicName = item != null ? item.getTopic() : SharedConstants.UNKNOWN;
            String subtopicName = item != null ? item.getSubtopic() : null;
            SubjectTopicService.HierarchyIds ids = subjectTopicService.resolveOrCreateByName(
                    subjectName, topicName, subtopicName, job.getTenantId());

            Question question = Question.builder()
                    .subjectId(ids.subjectId())
                    .topicId(ids.topicId())
                    .subtopicId(ids.subtopicId())
                    .subject(subjectName)
                    .topic(topicName)
                    .subtopic(subtopicName)
                    .difficulty(resolveDifficulty(raw.difficulty, item))
                    .cognitiveLevel(resolveCognitiveLevel(raw.cognitiveLevel, item))
                    .questionType(resolveQuestionType(raw.questionType, item))
                    .content(raw.content)
                    .answerKey(raw.answerKey)
                    .explanation(raw.explanation)
                    .options(raw.options)
                    .sourceReferences("AI-generated (Bedrock batch) via " + job.getModelUsed())
                    .state(QuestionStateConstants.DRAFT)
                    .authorId(job.getInitiatedBy())
                    .build();
            question.setTenantId(job.getTenantId());

            Question saved = questionRepository.save(question);
            generateAndSaveEmbedding(saved.getId(), raw.content);

            return saved.getId();
        } catch (Exception _) {
            log.error("Failed to persist batch question");
            return null;
        }
    }

    private void generateAndSaveEmbedding(UUID questionId, String content) {
        try {
            float[] embedding = embeddingService.embed(content);
            if (embedding != null && embedding.length > 0) {
                questionRepository.updateEmbedding(questionId, EmbeddingUtils.embeddingToString(embedding));
            }
        } catch (Exception _) {
            log.warn("Embedding failed for batch question {}", questionId);
        }
    }

    private String resolveDifficulty(String rawDifficulty, BatchGenerationRequest.BatchItem item) {
        if (rawDifficulty != null) {
            return rawDifficulty;
        }
        return (item != null && item.getDifficulty() != null)
                ? item.getDifficulty()
                : QuestionTaxonomyConstants.Difficulty.MEDIUM;
    }

    private String resolveCognitiveLevel(String rawCognitiveLevel, BatchGenerationRequest.BatchItem item) {
        if (rawCognitiveLevel != null) {
            return rawCognitiveLevel;
        }
        return (item != null && item.getCognitiveLevel() != null)
                ? item.getCognitiveLevel()
                : QuestionTaxonomyConstants.CognitiveLevel.APPLY;
    }

    private String resolveQuestionType(String rawQuestionType, BatchGenerationRequest.BatchItem item) {
        if (rawQuestionType != null) {
            return rawQuestionType;
        }
        return (item != null && item.getQuestionType() != null)
                ? item.getQuestionType()
                : QuestionTypeConstants.SINGLE_MCQ;
    }

    private List<RawQuestion> parseModelOutput(JsonNode modelOutput) {
        try {
            JsonNode output = modelOutput.path("output").path(SharedConstants.MESSAGE).path(SharedConstants.CONTENT);
            if (output.isArray() && !output.isEmpty()) {
                return parseJsonArray(extractTextFromBlocks(output));
            }

            JsonNode content = modelOutput.get(SharedConstants.CONTENT);
            if (content != null && content.isArray()) {
                return parseJsonArray(extractTextFromBlocks(content));
            }

            String fallbackText = modelOutput.has("text")
                    ? modelOutput.get("text").asText()
                    : modelOutput.toString();
            return parseJsonArray(fallbackText);
        } catch (Exception _) {
            log.warn("Failed to parse model output");
            return List.of();
        }
    }

    private String extractTextFromBlocks(JsonNode arrayNode) {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : arrayNode) {
            if (block.has("text")) {
                text.append(block.get("text").asText());
            }
        }
        return text.toString();
    }

    private List<RawQuestion> parseJsonArray(String text) {
        if (text == null || text.isBlank()) return List.of();
        String cleaned = text.strip();
        if (cleaned.startsWith("```")) {
            int nl = cleaned.indexOf('\n');
            if (nl > 0) cleaned = cleaned.substring(nl + 1);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            cleaned = cleaned.strip();
        }
        try {
            if (cleaned.startsWith("[")) return objectMapper.readValue(cleaned, new TypeReference<>() {});
            if (cleaned.startsWith("{")) return List.of(objectMapper.readValue(cleaned, RawQuestion.class));
        } catch (Exception _) { log.warn("JSON parse failed"); }
        return List.of();
    }

    private boolean isValid(RawQuestion raw, String expectedType) {
        if (raw.content == null || raw.content.isBlank()) return false;
        if (raw.answerKey == null || raw.answerKey.isBlank()) return false;
        String type = raw.questionType != null ? raw.questionType : expectedType;
        if (MCQ_TYPES.contains(type)) {
            if (raw.options == null || raw.options.size() != 4) return false;
            long correct = raw.options.stream().filter(QuestionOption::isCorrect).count();
            if (QuestionTypeConstants.SINGLE_MCQ.equals(type) && correct != 1) return false;
            return !QuestionTypeConstants.MULTI_MCQ.equals(type) || correct >= 2;
        }
        return true;
    }

    private boolean isDuplicate(String content, String subject, String tenantId) {
        try {
            similarityDetectionService.enforceNoDuplicate(content, subject, tenantId);
            return false;
        } catch (com.examplatform.questionbank.exception.SimilarQuestionException _) {
            return true;
        } catch (Exception _) { return false; }
    }

    private String serializeIds(List<UUID> ids) {
        try { return objectMapper.writeValueAsString(ids); } catch (Exception _) { return "[]"; }
    }

    @Transactional
    public BatchJobResponse cancelJob(UUID jobId, String tenantId) {
        BatchGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Batch job not found: " + jobId));
        if (!job.getTenantId().equals(tenantId)) throw new IllegalArgumentException("Job does not belong to this tenant");
        if (job.getStatus() == BatchJobStatus.PENDING || job.getStatus() == BatchJobStatus.PROCESSING) {
            if (job.getBedrockJobArn() != null) {
                try {
                    bedrockClient.stopModelInvocationJob(b -> b.jobIdentifier(job.getBedrockJobArn()));
                } catch (Exception _) {
                    log.warn("Failed to stop Bedrock job");
                }
            }
            job.setStatus(BatchJobStatus.CANCELLED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
        return BatchJobResponse.from(job);
    }

    private record RawQuestion(String content, String answerKey, String explanation,
                               List<QuestionOption> options, String difficulty,
                               String cognitiveLevel, String questionType) {}
}
