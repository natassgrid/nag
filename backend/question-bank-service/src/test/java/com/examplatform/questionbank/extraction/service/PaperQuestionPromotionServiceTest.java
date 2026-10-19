/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.service;

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsRequest;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsResponse;
import com.examplatform.questionbank.extraction.repository.ExtractedQuestionRepository;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.questionbank.translation.service.TranslationWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaperQuestionPromotionService Unit Tests")
class PaperQuestionPromotionServiceTest {

    @Mock
    private QuestionPaperMetadataRepository metadataRepository;

    @Mock
    private ExtractedQuestionRepository extractedQuestionRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private TranslationWorkflowService translationWorkflowService;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubtopicRepository subtopicRepository;

    private PaperQuestionPromotionService promotionService;

    private final String tenantId = "tenant-promo";
    private final UUID paperId = UUID.randomUUID();
    private final UUID promoterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        promotionService = new PaperQuestionPromotionService(
                metadataRepository,
                extractedQuestionRepository,
                questionService,
                translationWorkflowService,
                subjectRepository,
                topicRepository,
                subtopicRepository
        );
    }

    @Test
    @DisplayName("promoteQuestions promotes APPROVED questions and creates Hindi translation")
    void testPromoteQuestionsSuccessfully() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("SSC CGL 2024")
                .examYear(2024)
                .defaultSubjectId(10L)
                .defaultTopicId(20L)
                .defaultSubtopicId(30L)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));

        UUID eqId = UUID.randomUUID();
        ExtractedQuestion eq = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .content("What is the capital of India?")
                .questionType("SINGLE_MCQ")
                .options(new ArrayList<>(List.of(
                        new QuestionOption("A", "Mumbai", false, null, null),
                        new QuestionOption("B", "New Delhi", true, null, null),
                        new QuestionOption("C", "Kolkata", false, null, null),
                        new QuestionOption("D", "Chennai", false, null, null)
                )))
                .answerKey("B")
                .explanation("New Delhi is the official capital.")
                .translations(Map.of("hi", Map.of(
                        "content", "भारत की राजधानी क्या है?",
                        "options", List.of(new QuestionOption("B", "नई दिल्ली", true, null, null))
                )))
                .status(ExtractedQuestionStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(eq, "id", eqId);

        when(extractedQuestionRepository.findByPaperMetadataIdAndStatusAndTenantIdOrderByQuestionNumberAsc(
                paperId, ExtractedQuestionStatus.APPROVED, tenantId))
                .thenReturn(List.of(eq));

        UUID createdQuestionId = UUID.randomUUID();
        QuestionResponse qResp = QuestionResponse.builder().id(createdQuestionId).build();
        when(questionService.createQuestion(any(CreateQuestionRequest.class), eq(promoterId), eq(tenantId)))
                .thenReturn(qResp);

        PromoteQuestionsRequest req = PromoteQuestionsRequest.builder()
                .createTranslations(true)
                .build();

        PromoteQuestionsResponse response = promotionService.promoteQuestions(paperId, req, promoterId, tenantId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalPromoted()).isEqualTo(1);
        assertThat(response.getPromotedQuestionIds()).containsExactly(createdQuestionId);

        assertThat(eq.getStatus()).isEqualTo(ExtractedQuestionStatus.PROMOTED);
        assertThat(eq.getPromotedQuestionId()).isEqualTo(createdQuestionId);

        verify(translationWorkflowService).upsertTranslation(
                eq(createdQuestionId),
                eq("hi"),
                eq("भारत की राजधानी क्या है?"),
                any(),
                any(),
                any(),
                eq(promoterId),
                any(),
                eq(tenantId)
        );
    }
}
