// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.microlesson.domain.MicroLessonStatus;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MicroLessonGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonGeneratorService.class);
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    private final ChatClient chatClient;
    private final MicroLessonPromptBuilder promptBuilder;
    private final KaTexValidator kaTexValidator;
    private final SubtopicMicroLessonRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubtopicMicroLesson generateForSubtopic(
            Long subjectId, String subject,
            Long topicId, String topic,
            Long subtopicId, String subtopic,
            String targetExam) {
        return generateForSubtopic(subjectId, subject, topicId, topic, subtopicId, subtopic, targetExam, null);
    }

    @Transactional
    public SubtopicMicroLesson generateForSubtopic(
            Long subjectId, String subject,
            Long topicId, String topic,
            Long subtopicId, String subtopic,
            String targetExam,
            String referenceText) {

        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";
        log.info("Generating remedial micro-lesson for subtopic: '{}' in topic: '{}' (Exam: {})", subtopic, topic, exam);

        MicroLessonContentDto content;
        try {
            String userPrompt = promptBuilder.buildUserPrompt(subject, topic, subtopic, exam, referenceText);
            String rawResponse = chatClient.prompt()
                    .system(MicroLessonPromptBuilder.SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            content = parseAndValidate(rawResponse);
        } catch (Exception e) {
            log.warn("AI generation failed for subtopic '{}': {}. Generating high-yield pedagogical fallback template.", subtopic, e.getMessage());
            content = buildFallbackContent(subject, topic, subtopic, exam);
        }

        // Determine next version
        int nextVersion = repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(subtopicId, exam)
                .map(prev -> prev.getContentVersion() + 1)
                .orElse(1);

        String cheatSheetJson;
        String stepJson;
        try {
            cheatSheetJson = objectMapper.writeValueAsString(content.cheatSheet());
            stepJson = objectMapper.writeValueAsString(content.stepByStepBreakdown());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize micro-lesson content", e);
        }

        SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .subtopicId(subtopicId)
                .subject(subject != null ? subject : "General")
                .topic(topic != null ? topic : "General")
                .subtopic(subtopic)
                .targetExam(exam)
                .coreConcept(content.coreConcept())
                .trap(content.trap())
                .cheatSheetJson(cheatSheetJson)
                .stepByStepBreakdownJson(stepJson)
                .status(MicroLessonStatus.AI_GENERATED.name())
                .contentVersion(nextVersion)
                .build();

        return repository.save(entity);
    }

    private MicroLessonContentDto parseAndValidate(String responseText) throws JsonProcessingException {
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalArgumentException("Empty response from AI chat client");
        }

        String jsonText = responseText.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(jsonText);
        if (matcher.find()) {
            jsonText = matcher.group(1).trim();
        }

        MicroLessonContentDto content = objectMapper.readValue(jsonText, MicroLessonContentDto.class);
        List<String> errors = kaTexValidator.validateContent(content);
        if (!errors.isEmpty()) {
            log.warn("KaTeX / validation warnings in generated micro-lesson: {}", errors);
        }
        return content;
    }

    public MicroLessonContentDto buildFallbackContent(String subject, String topic, String subtopic, String targetExam) {
        String coreConcept = String.format(
                "%s forms the bedrock principle connecting %s within %s. Think of it like adjusting the tension in a traditional Indian bicycle chain; too loose causes slipping while too tight restricts motion. Mastering this relation allows swift mental decomposition of complex numerical problems.",
                subtopic, topic, subject
        );

        String trap = String.format(
                "Students frequently forget sign conventions and fail to verify boundary units for %s during high-pressure timed exams.",
                subtopic
        );

        List<String> cheatSheet = List.of(
                "$$\\Delta K + \\Delta U = W_{ext}$$",
                "$$I = \\int r^2 \\, dm$$"
        );

        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                String.format("Evaluate the governing parameter for %s given standard baseline equilibrium conditions.", subtopic),
                List.of(
                        "Step 1: Write down the primary governing conservation equation and mark known boundary conditions.",
                        "Step 2: Substitute dimensionally consistent SI values and simplify algebraic radicals.",
                        "Step 3: Check dimensional homogeneity and verify limiting edge cases."
                ),
                "4.5 \\times 10^3 \\text{ J}"
        );

        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    public MicroLessonResponse toResponse(SubtopicMicroLesson entity) {
        List<String> cheatSheet = Collections.emptyList();
        StepByStepBreakdownDto breakdown = null;

        if (entity.getCheatSheetJson() != null && !entity.getCheatSheetJson().isBlank()) {
            try {
                cheatSheet = objectMapper.readValue(entity.getCheatSheetJson(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cheatSheet for micro-lesson {}: {}", entity.getId(), e.getMessage());
            }
        }

        if (entity.getStepByStepBreakdownJson() != null && !entity.getStepByStepBreakdownJson().isBlank()) {
            try {
                breakdown = objectMapper.readValue(entity.getStepByStepBreakdownJson(), StepByStepBreakdownDto.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse stepByStepBreakdown for micro-lesson {}: {}", entity.getId(), e.getMessage());
            }
        }

        return new MicroLessonResponse(
                entity.getId(),
                entity.getSubjectId(),
                entity.getTopicId(),
                entity.getSubtopicId(),
                entity.getSubject(),
                entity.getTopic(),
                entity.getSubtopic(),
                entity.getTargetExam(),
                entity.getCoreConcept(),
                entity.getTrap(),
                cheatSheet,
                breakdown,
                entity.getStatus(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getReviewComments(),
                entity.getContentVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
