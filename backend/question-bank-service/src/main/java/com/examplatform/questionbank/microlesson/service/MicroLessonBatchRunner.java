// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MicroLessonBatchRunner {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonBatchRunner.class);

    private final SubtopicRepository subtopicRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubtopicMicroLessonRepository microLessonRepository;
    private final MicroLessonGeneratorService generatorService;

    /**
     * Executes synchronous or triggered batch generation for specified or missing subtopics.
     */
    public BatchGenerateResponse runBatchGeneration(BatchGenerateMicroLessonsRequest request) {
        String targetExam = (request != null && request.targetExam() != null && !request.targetExam().isBlank())
                ? request.targetExam()
                : "JEE_MAIN";

        int max = (request != null && request.maxCount() != null && request.maxCount() > 0)
                ? request.maxCount()
                : 20;

        List<Subtopic> candidates = new ArrayList<>();
        if (request != null && request.subtopicIds() != null && !request.subtopicIds().isEmpty()) {
            candidates = subtopicRepository.findAllById(request.subtopicIds());
        } else if (request != null && request.topicId() != null) {
            candidates = subtopicRepository.findByTopicIdAndTenantId(request.topicId(), "default");
        } else {
            // Find all active subtopics up to max
            List<Subtopic> all = subtopicRepository.findAll();
            for (Subtopic s : all) {
                if (s.isActive()) {
                    candidates.add(s);
                    if (candidates.size() >= max * 2) break;
                }
            }
        }

        int queued = 0;
        int generated = 0;
        int failed = 0;
        List<String> generatedNames = new ArrayList<>();
        Set<String> approvedStatuses = Set.of(MicroLessonStatus.APPROVED.name(), MicroLessonStatus.PUBLISHED.name());

        for (Subtopic sub : candidates) {
            if (generated >= max) break;

            boolean alreadyCovered = microLessonRepository.existsBySubtopicIdAndTargetExamAndStatusIn(
                    sub.getId(), targetExam, approvedStatuses);

            if (alreadyCovered) {
                continue;
            }

            queued++;
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
                generated++;
                generatedNames.add(sub.getName());
            } catch (Exception e) {
                log.error("Failed to generate micro-lesson for subtopic {}: {}", sub.getName(), e.getMessage());
                failed++;
            }
        }

        return new BatchGenerateResponse(
                "COMPLETED",
                queued,
                generated,
                failed,
                generatedNames,
                String.format("Batch completed: %d generated, %d failed out of %d processed for exam %s",
                        generated, failed, queued, targetExam)
        );
    }

    /**
     * Nightly scheduled batch task.
     */
    @Scheduled(cron = "${app.microlesson.cron:0 0 2 * * ?}")
    public void runNightlyBatchJob() {
        log.info("Starting nightly automated micro-lesson generation task...");
        try {
            BatchGenerateResponse response = runBatchGeneration(new BatchGenerateMicroLessonsRequest(null, null, null, "JEE_MAIN", 50));
            log.info("Nightly micro-lesson batch completed: {}", response.message());
        } catch (Exception e) {
            log.error("Nightly micro-lesson generation failed: {}", e.getMessage(), e);
        }
    }
}
