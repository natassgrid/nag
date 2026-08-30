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
import software.amazon.awssdk.services.bedrock.model.CreateModelInvocationJobRequest;
import software.amazon.awssdk.services.bedrock.model.CreateModelInvocationJobResponse;
import software.amazon.awssdk.services.bedrock.model.GetModelInvocationJobRequest;
import software.amazon.awssdk.services.bedrock.model.GetModelInvocationJobResponse;
import software.amazon.awssdk.services.bedrock.model.ModelInvocationJobInputDataConfig;
import software.amazon.awssdk.services.bedrock.model.ModelInvocationJobOutputDataConfig;
import software.amazon.awssdk.services.bedrock.model.ModelInvocationJobS3InputDataConfig;
import software.amazon.awssdk.services.bedrock.model.ModelInvocationJobS3OutputDataConfig;
import software.amazon.awssdk.services.bedrock.model.ModelInvocationJobStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for async batch question generation using AWS Bedrock Batch Inference.
 *
 * <p>Multiple generation items (each with subject/topic/difficulty/type/count) are combined
 * into a single JSONL file, uploaded to S3, and processed as one Bedrock batch job.
 * This minimizes cost by avoiding per-job overhead charges.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockBatchService {

    private static final Set<String> MCQ_TYPES = Set.of("SINGLE_MCQ", "MULTI_MCQ");

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
     * All items are packed into one JSONL → one S3 upload → one Bedrock job.
     */
    @Transactional
    public BatchJobResponse submitBatchJob(BatchGenerationRequest request, UUID authorId, String tenantId) {
        List<BatchGenerationRequest.BatchItem> itemsList = request.getItems();
        int totalRequested = itemsList.stream().mapToInt(BatchGenerationRequest.BatchItem::getCount).sum();

        // Use the first item's difficulty for model selection (batch jobs use one model)
        String modelId = modelRouter.selectBatchModelId(itemsList.get(0).getDifficulty());

        // Serialize items to JSON for storage
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
            // Build JSONL: one record per item
            String jsonlContent = buildJsonlInput(itemsList);

            // Upload to S3
            String s3Key = s3InputPrefix + saved.getId() + "/input.jsonl";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(s3Key)
                            .contentType("application/jsonl")
                            .build(),
                    RequestBody.fromString(jsonlContent, StandardCharsets.UTF_8));

            // Create single Bedrock batch inference job
            String s3InputUri = "s3://" + s3Bucket + "/" + s3InputPrefix + saved.getId() + "/";
            String s3OutputUri = "s3://" + s3Bucket + "/" + s3OutputPrefix + saved.getId() + "/";

            CreateModelInvocationJobResponse bedrockResponse = bedrockClient.createModelInvocationJob(
                    CreateModelInvocationJobRequest.builder()
                            .jobName("exam-batch-" + saved.getId().toString().substring(0, 8))
                            .modelId(modelId)
                            .roleArn(bedrockRoleArn)
                            .inputDataConfig(ModelInvocationJobInputDataConfig.builder()
                                    .s3InputDataConfig(ModelInvocationJobS3InputDataConfig.builder()
                                            .s3Uri(s3InputUri).build())
                                    .build())
                            .outputDataConfig(ModelInvocationJobOutputDataConfig.builder()
                                    .s3OutputDataConfig(ModelInvocationJobS3OutputDataConfig.builder()
                                            .s3Uri(s3OutputUri).build())
                                    .build())
                            .build());

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

            String systemPrompt = buildSystemPrompt(item);
            String userPrompt = buildUserPrompt(item);
            String modelInput = buildModelInput(systemPrompt, userPrompt);

            try {
                JsonNode inputNode = objectMapper.readTree(modelInput);
                var record = objectMapper.createObjectNode();
                record.put("recordId", recordId);
                record.set("modelInput", inputNode);
                jsonl.append(objectMapper.writeValueAsString(record)).append("\n");
            } catch (Exception e) {
                log.error("Failed to build JSONL record {}: {}", recordId, e.getMessage());
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

    private String buildSystemPrompt(BatchGenerationRequest.BatchItem item) {
        return "You are an expert examination question generator for Indian competitive examinations. "
                + "Generate high-quality questions in structured JSON format. "
                + "Rules: "
                + "- Generate questions strictly matching the specified type, difficulty, and cognitive level. "
                + "- For MCQ (SINGLE_MCQ): exactly 4 options with ids A, B, C, D. Set isCorrect:true on EXACTLY ONE. answerKey must be the correct option id. "
                + "- For MSQ (MULTI_MCQ): exactly 4 options (A, B, C, D), 2 or more correct. "
                + "- For NUMERICAL: no options, answerKey is the numeric value. "
                + "- For DESCRIPTIVE: no options, answerKey contains the model answer. "
                + "- Always provide a clear explanation. "
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
        } catch (Exception e) {
            return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }

    // ─── Polling & Result Processing ─────────────────────────────────────────────

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void pollProcessingJobs() {
        List<BatchGenerationJob> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(BatchJobStatus.PROCESSING);
        for (BatchGenerationJob job : jobs) {
            try {
                pollJob(job);
            } catch (Exception e) {
                log.error("Error polling batch job {}: {}", job.getId(), e.getMessage());
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
                GetModelInvocationJobRequest.builder().jobIdentifier(job.getBedrockJobArn()).build());

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

    /**
     * Downloads output JSONL, parses each record's model output, validates questions,
     * and persists them. Each output record's recordId maps back to the original item
     * for correct subject/topic metadata.
     */
    private void processCompletedJob(BatchGenerationJob job) {
        log.info("Processing completed batch job: id={}", job.getId());

        // Deserialize items for metadata
        List<BatchGenerationRequest.BatchItem> items;
        try {
            items = objectMapper.readValue(job.getItems(),
                    new TypeReference<List<BatchGenerationRequest.BatchItem>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize job items: {}", e.getMessage());
            job.setStatus(BatchJobStatus.FAILED);
            job.setErrorMessage("Failed to deserialize items");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            return;
        }

        List<UUID> generatedIds = new ArrayList<>();
        int totalGenerated = 0, totalFailed = 0, totalDuplicates = 0;

        try {
            String outputPrefix = s3OutputPrefix + job.getId() + "/";
            var listResp = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(s3Bucket).prefix(outputPrefix).build());

            for (S3Object s3Obj : listResp.contents()) {
                if (!s3Obj.key().endsWith(".jsonl")) continue;

                var getResp = s3Client.getObject(GetObjectRequest.builder()
                        .bucket(s3Bucket).key(s3Obj.key()).build());

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(getResp, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        try {
                            JsonNode record = objectMapper.readTree(line);
                            String recordId = record.path("recordId").asText("");
                            JsonNode modelOutput = record.get("modelOutput");

                            if (modelOutput == null || modelOutput.isNull()) {
                                totalFailed++;
                                continue;
                            }

                            // Map recordId back to item for metadata
                            int itemIndex = parseRecordIndex(recordId);
                            BatchGenerationRequest.BatchItem item = (itemIndex >= 0 && itemIndex < items.size())
                                    ? items.get(itemIndex) : null;

                            List<RawQuestion> questions = parseModelOutput(modelOutput);

                            for (RawQuestion raw : questions) {
                                String qType = item != null ? item.getQuestionType() : "SINGLE_MCQ";
                                if (!isValid(raw, qType)) { totalFailed++; continue; }

                                if (job.isAvoidDuplicates() && item != null) {
                                    if (isDuplicate(raw.content, item.getSubject(), job.getTenantId())) {
                                        totalDuplicates++;
                                        continue;
                                    }
                                }

                                UUID savedId = persistQuestion(raw, item, job);
                                if (savedId != null) {
                                    generatedIds.add(savedId);
                                    totalGenerated++;
                                } else {
                                    totalFailed++;
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse output record: {}", e.getMessage());
                            totalFailed++;
                        }
                    }
                }
            }

            job.setTotalGenerated(totalGenerated);
            job.setTotalFailed(totalFailed);
            job.setTotalDuplicates(totalDuplicates);
            job.setGeneratedQuestionIds(serializeIds(generatedIds));
            job.setStatus(totalGenerated > 0
                    ? (totalFailed > 0 ? BatchJobStatus.PARTIALLY_COMPLETED : BatchJobStatus.COMPLETED)
                    : BatchJobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            if (totalGenerated == 0) job.setErrorMessage("All records failed validation or duplicate detection");
            jobRepository.save(job);

            log.info("Batch job done: id={}, generated={}, failed={}, duplicates={}",
                    job.getId(), totalGenerated, totalFailed, totalDuplicates);
        } catch (Exception e) {
            log.error("Failed processing batch job {}: {}", job.getId(), e.getMessage(), e);
            job.setStatus(BatchJobStatus.FAILED);
            job.setErrorMessage("Processing failed: " + e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    private int parseRecordIndex(String recordId) {
        // Format: "REC-0003" → 3
        try {
            return Integer.parseInt(recordId.replace("REC-", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private UUID persistQuestion(RawQuestion raw, BatchGenerationRequest.BatchItem item, BatchGenerationJob job) {
        try {
            String subjectName = item != null ? item.getSubject() : "Unknown";
            String topicName = item != null ? item.getTopic() : "Unknown";
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
                    .difficulty(raw.difficulty != null ? raw.difficulty : (item != null ? item.getDifficulty() : "MEDIUM"))
                    .cognitiveLevel(raw.cognitiveLevel != null ? raw.cognitiveLevel : (item != null ? item.getCognitiveLevel() : "APPLY"))
                    .questionType(raw.questionType != null ? raw.questionType : (item != null ? item.getQuestionType() : "SINGLE_MCQ"))
                    .content(raw.content)
                    .answerKey(raw.answerKey)
                    .explanation(raw.explanation)
                    .options(raw.options)
                    .references("AI-generated (Bedrock batch) via " + job.getModelUsed())
                    .state("DRAFT")
                    .authorId(job.getInitiatedBy())
                    .build();
            question.setTenantId(job.getTenantId());

            Question saved = questionRepository.save(question);

            try {
                float[] embedding = embeddingService.embed(raw.content);
                if (embedding != null && embedding.length > 0) {
                    questionRepository.updateEmbedding(saved.getId(), EmbeddingUtils.embeddingToString(embedding));
                }
            } catch (Exception e) {
                log.warn("Embedding failed for batch question {}: {}", saved.getId(), e.getMessage());
            }

            return saved.getId();
        } catch (Exception e) {
            log.error("Failed to persist batch question: {}", e.getMessage());
            return null;
        }
    }

    private List<RawQuestion> parseModelOutput(JsonNode modelOutput) {
        try {
            // Converse API: {"output":{"message":{"content":[{"text":"..."}]}}}
            JsonNode output = modelOutput.path("output").path("message").path("content");
            if (output.isArray() && !output.isEmpty()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode block : output) { if (block.has("text")) text.append(block.get("text").asText()); }
                return parseJsonArray(text.toString());
            }
            // Fallback: {"content":[{"text":"..."}]}
            JsonNode content = modelOutput.get("content");
            if (content != null && content.isArray()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode block : content) { if (block.has("text")) text.append(block.get("text").asText()); }
                return parseJsonArray(text.toString());
            }
            return parseJsonArray(modelOutput.has("text") ? modelOutput.get("text").asText() : modelOutput.toString());
        } catch (Exception e) {
            log.warn("Failed to parse model output: {}", e.getMessage());
            return List.of();
        }
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
        } catch (Exception e) { log.warn("JSON parse failed: {}", e.getMessage()); }
        return List.of();
    }

    private boolean isValid(RawQuestion raw, String expectedType) {
        if (raw.content == null || raw.content.isBlank()) return false;
        if (raw.answerKey == null || raw.answerKey.isBlank()) return false;
        String type = raw.questionType != null ? raw.questionType : expectedType;
        if (MCQ_TYPES.contains(type)) {
            if (raw.options == null || raw.options.size() != 4) return false;
            long correct = raw.options.stream().filter(QuestionOption::isCorrect).count();
            if ("SINGLE_MCQ".equals(type) && correct != 1) return false;
            if ("MULTI_MCQ".equals(type) && correct < 2) return false;
        }
        return true;
    }

    private boolean isDuplicate(String content, String subject, String tenantId) {
        try {
            similarityDetectionService.enforceNoDuplicate(content, subject, tenantId);
            return false;
        } catch (com.examplatform.questionbank.exception.SimilarQuestionException e) {
            return true;
        } catch (Exception e) { return false; }
    }

    private String serializeIds(List<UUID> ids) {
        try { return objectMapper.writeValueAsString(ids); } catch (Exception e) { return "[]"; }
    }

    @Transactional
    public BatchJobResponse cancelJob(UUID jobId, String tenantId) {
        BatchGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Batch job not found: " + jobId));
        if (!job.getTenantId().equals(tenantId)) throw new IllegalArgumentException("Job does not belong to this tenant");
        if (job.getStatus() == BatchJobStatus.PENDING || job.getStatus() == BatchJobStatus.PROCESSING) {
            if (job.getBedrockJobArn() != null) {
                try { bedrockClient.stopModelInvocationJob(b -> b.jobIdentifier(job.getBedrockJobArn())); }
                catch (Exception e) { log.warn("Failed to stop Bedrock job: {}", e.getMessage()); }
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
