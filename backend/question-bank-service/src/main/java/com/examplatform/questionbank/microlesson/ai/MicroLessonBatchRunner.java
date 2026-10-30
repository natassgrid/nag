// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.microlesson.domain.MicroLessonStatus;
import com.examplatform.questionbank.microlesson.dto.BatchGenerateMicroLessonsRequest;
import com.examplatform.questionbank.microlesson.dto.BatchGenerateResponse;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.shared.constants.SharedConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class MicroLessonBatchRunner {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonBatchRunner.class);

    private final SubtopicRepository subtopicRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubtopicMicroLessonRepository microLessonRepository;
    private final MicroLessonGeneratorService generatorService;

    private final Map<String, BatchJobState> jobStates = new ConcurrentHashMap<>();
    private final AtomicReference<String> activeBatchId = new AtomicReference<>();

    @Getter
    @Setter
    public static class BatchJobState {
        private final String batchId;
        private final String targetExam;
        private final int totalQueued;
        private final AtomicInteger totalGenerated = new AtomicInteger(0);
        private final AtomicInteger totalFailed = new AtomicInteger(0);
        private final List<String> generatedSubtopics = new CopyOnWriteArrayList<>();
        private final List<String> failedSubtopics = new CopyOnWriteArrayList<>();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile String status; // QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
        private volatile String currentSubtopic;
        private volatile String message;
        private final Instant startTime = Instant.now();
        private volatile Instant completedTime;

        public BatchJobState(String batchId, String targetExam, int totalQueued, String status, String message) {
            this.batchId = batchId;
            this.targetExam = targetExam;
            this.totalQueued = totalQueued;
            this.status = status;
            this.message = message;
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        public void cancel() {
            this.cancelled.set(true);
            this.status = SharedConstants.CANCELLED;
            this.message = "Generation cancelled by user.";
            this.completedTime = Instant.now();
        }

        public BatchGenerateResponse toResponse() {
            int queued = totalQueued;
            int gen = totalGenerated.get();
            int fail = totalFailed.get();
            int percent = queued > 0 ? (gen + fail) * 100 / queued : ((SharedConstants.COMPLETED.equals(status)) ? 100 : 0);
            if (percent > 100) percent = 100;

            return new BatchGenerateResponse(
                    batchId,
                    status,
                    queued,
                    gen,
                    fail,
                    percent,
                    currentSubtopic,
                    new ArrayList<>(generatedSubtopics),
                    new ArrayList<>(failedSubtopics),
                    message
            );
        }
    }

    /**
     * Resolves candidate subtopics based on user filter parameters.
     */
    private List<Subtopic> resolveCandidates(BatchGenerateMicroLessonsRequest request, int max, String targetExam, boolean forceRegenerate) {
        List<Subtopic> rawCandidates = new ArrayList<>();
        if (request != null && request.subtopicIds() != null && !request.subtopicIds().isEmpty()) {
            rawCandidates = subtopicRepository.findAllById(request.subtopicIds());
        } else if (request != null && request.topicId() != null) {
            rawCandidates = subtopicRepository.findByTopicIdAndTenantId(request.topicId(), SharedConstants.DEFAULT_TENANT);
        } else if (request != null && request.subjectId() != null) {
            List<Topic> topics = topicRepository.findBySubjectIdAndTenantId(request.subjectId(), SharedConstants.DEFAULT_TENANT);
            for (Topic t : topics) {
                rawCandidates.addAll(subtopicRepository.findByTopicIdAndTenantId(t.getId(), SharedConstants.DEFAULT_TENANT));
            }
        } else {
            List<Subtopic> all = subtopicRepository.findAll();
            for (Subtopic s : all) {
                if (s.isActive()) {
                    rawCandidates.add(s);
                    if (rawCandidates.size() >= max * 3) break;
                }
            }
        }

        Set<String> approvedStatuses = Set.of(MicroLessonStatus.APPROVED.name(), MicroLessonStatus.PUBLISHED.name());
        List<Subtopic> resolved = new ArrayList<>();
        for (Subtopic sub : rawCandidates) {
            if (resolved.size() >= max) break;
            if (!forceRegenerate) {
                boolean alreadyCovered = microLessonRepository.existsBySubtopicIdAndStatusIn(
                        sub.getId(), approvedStatuses);
                if (alreadyCovered) {
                    continue;
                }
            }
            resolved.add(sub);
        }
        return resolved;
    }

    /**
     * Starts an asynchronous batch generation job and returns immediately with tracking details.
     */
    public BatchGenerateResponse startBatchJob(BatchGenerateMicroLessonsRequest request) {
        String targetExam = (request != null && request.targetExam() != null && !request.targetExam().isBlank())
                ? request.targetExam()
                : SharedConstants.JEE_MAIN;

        int max = (request != null && request.maxCount() != null && request.maxCount() > 0)
                ? request.maxCount()
                : 20;

        boolean forceRegenerate = (request != null && Boolean.TRUE.equals(request.forceRegenerate()));

        List<Subtopic> candidates = resolveCandidates(request, max, targetExam, forceRegenerate);
        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);

        if (candidates.isEmpty()) {
            BatchJobState emptyJob = new BatchJobState(
                    batchId,
                    targetExam,
                    0,
                SharedConstants.COMPLETED,
                    "No subtopics require generation for the chosen criteria (all already covered)."
            );
            emptyJob.setCompletedTime(Instant.now());
            jobStates.put(batchId, emptyJob);
            activeBatchId.set(batchId);
            return emptyJob.toResponse();
        }

        BatchJobState jobState = new BatchJobState(
                batchId,
                targetExam,
                candidates.size(),
            SharedConstants.IN_PROGRESS,
                String.format("Batch job queued with %d subtopics for %s", candidates.size(), targetExam)
        );
        jobStates.put(batchId, jobState);
        activeBatchId.set(batchId);

        CompletableFuture.runAsync(() -> executeBatchLoop(jobState, candidates, targetExam));

        return jobState.toResponse();
    }

    private void executeBatchLoop(BatchJobState state, List<Subtopic> candidates, String targetExam) {
        log.info("Started async batch generation job {} for {} subtopics (Exam: {})",
                state.getBatchId(), candidates.size(), targetExam);

        try {
            for (Subtopic sub : candidates) {
                if (state.isCancelled()) {
                    log.info("Batch job {} cancelled during execution.", state.getBatchId());
                    return;
                }

                state.setCurrentSubtopic(sub.getName());
                try {
                    Topic topic = topicRepository.findById(sub.getTopicId()).orElse(null);
                    String topicName = (topic != null) ? topic.getName() : "General Topic";
                    Long subjectId = (topic != null) ? topic.getSubjectId() : 1L;
                    Subject subject = (topic != null) ? subjectRepository.findById(subjectId).orElse(null) : null;
                    String subjectName = (subject != null) ? subject.getName() : "General Subject";

                    generatorService.generateForSubtopic(
                            subjectId, subjectName,
                            sub.getTopicId(), topicName,
                            sub.getId(), sub.getName(),
                            targetExam
                    );
                    state.getTotalGenerated().incrementAndGet();
                    state.getGeneratedSubtopics().add(sub.getName());
                } catch (Exception e) {
                    log.error("Failed to generate micro-lesson for subtopic {}: {}", sub.getName(), e.getMessage());
                    state.getTotalFailed().incrementAndGet();
                    state.getFailedSubtopics().add(sub.getName());
                }
            }

            state.setCurrentSubtopic(null);
            state.setStatus(SharedConstants.COMPLETED);
            state.setMessage(String.format("Batch completed: %d generated, %d failed out of %d processed for exam %s",
                    state.getTotalGenerated().get(), state.getTotalFailed().get(), state.getTotalQueued(), targetExam));
        } catch (Exception e) {
            log.error("Fatal error during batch generation job {}: {}", state.getBatchId(), e.getMessage(), e);
            state.setStatus("FAILED");
            state.setMessage("Batch failed: " + e.getMessage());
        } finally {
            state.setCompletedTime(Instant.now());
            log.info("Completed batch generation job {}: status={}", state.getBatchId(), state.getStatus());
        }
    }

    /**
     * Gets status of a specific batch generation job.
     */
    public Optional<BatchGenerateResponse> getJobStatus(String batchId) {
        BatchJobState state = jobStates.get(batchId);
        return state != null ? Optional.of(state.toResponse()) : Optional.empty();
    }

    /**
     * Retrieves the currently active batch job or the one completed within the last 30 seconds.
     */
    public BatchGenerateResponse getActiveJob() {
        String currentId = activeBatchId.get();
        if (currentId == null) {
            return null;
        }
        BatchJobState state = jobStates.get(currentId);
        if (state == null) {
            return null;
        }

        if (SharedConstants.IN_PROGRESS.equals(state.getStatus()) || "QUEUED".equals(state.getStatus())) {
            return state.toResponse();
        }

        // Keep recently completed/failed/cancelled accessible for 30 seconds for UI detection
        if (state.getCompletedTime() != null &&
                Duration.between(state.getCompletedTime(), Instant.now()).getSeconds() < 30) {
            return state.toResponse();
        }

        return null;
    }

    /**
     * Cancels an ongoing batch job.
     */
    public Optional<BatchGenerateResponse> cancelJob(String batchId) {
        BatchJobState state = jobStates.get(batchId);
        if (state == null) {
            return Optional.empty();
        }
        state.cancel();
        return Optional.of(state.toResponse());
    }

    /**
     * Executes synchronous or triggered batch generation for specified or missing subtopics.
     */
    public BatchGenerateResponse runBatchGeneration(BatchGenerateMicroLessonsRequest request) {
        return startBatchJob(request);
    }

    /**
     * Nightly scheduled batch task.
     */
    @Scheduled(cron = "${app.microlesson.cron:0 0 2 * * ?}")
    public void runNightlyBatchJob() {
        log.info("Starting nightly automated micro-lesson generation task...");
        try {
            BatchGenerateResponse response = runBatchGeneration(new BatchGenerateMicroLessonsRequest(null, null, null,
                SharedConstants.JEE_MAIN, 50));
            log.info("Nightly micro-lesson batch triggered: {}", response.message());
        } catch (Exception e) {
            log.error("Nightly micro-lesson generation failed: {}", e.getMessage(), e);
        }
    }
}
