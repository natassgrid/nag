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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BatchTranslationJobResponseDto;
import com.examplatform.papergenerator.dto.PaperTranslateRequest;
import com.examplatform.papergenerator.dto.PaperTranslateResponse;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperTranslationServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private QuestionBankClient questionBankClient;

    private ObjectMapper objectMapper;
    private PaperTranslationService translationService;

    private UUID paperId;
    private UUID questionId1;
    private UUID questionId2;
    private UUID userId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        translationService = new PaperTranslationService(paperRepository, questionBankClient, objectMapper);

        paperId = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        userId = UUID.randomUUID();
        tenantId = "default";
    }

    @Test
    @DisplayName("Should extract question IDs from paper definition JSON and initiate batch translation")
    void shouldInitiatePaperTranslationSuccessfully() {
        String paperJson = String.format("{\"questionIds\": [\"%s\", \"%s\"]}", questionId1, questionId2);
        Paper paper = Paper.builder()
                .name("Sample Paper")
                .status("APPROVED")
                .paperDefinitionJson(paperJson)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);

        when(paperRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));

        UUID jobId = UUID.randomUUID();
        BatchTranslationJobResponseDto jobDto = BatchTranslationJobResponseDto.builder()
                .id(jobId)
                .paperId(paperId)
                .status("PENDING")
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .overwriteExisting(true)
                .totalQuestions(2)
                .progressPercentage(0.0)
                .build();

        when(questionBankClient.triggerBatchTranslation(
                eq(paperId),
                eq(List.of(questionId1, questionId2)),
                any(PaperTranslateRequest.class),
                eq(userId),
                eq(tenantId)
        )).thenReturn(jobDto);

        PaperTranslateRequest request = PaperTranslateRequest.builder()
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .overwriteExisting(true)
                .build();

        PaperTranslateResponse response = translationService.translatePaper(paperId, request, userId, tenantId);

        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getPaperId()).isEqualTo(paperId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.isOverwriteExisting()).isTrue();

        verify(questionBankClient).triggerBatchTranslation(
                eq(paperId),
                eq(List.of(questionId1, questionId2)),
                eq(request),
                eq(userId),
                eq(tenantId)
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when paper does not exist")
    void shouldThrowWhenPaperNotFound() {
        when(paperRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.empty());
        when(paperRepository.findById(paperId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationService.translatePaper(paperId, new PaperTranslateRequest(), userId, tenantId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Paper not found");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when paper has no questions")
    void shouldThrowWhenPaperHasNoQuestions() {
        Paper paper = Paper.builder()
                .name("Empty Paper")
                .status("DRAFT")
                .paperDefinitionJson("{\"questionIds\": []}")
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);

        when(paperRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));

        assertThatThrownBy(() -> translationService.translatePaper(paperId, new PaperTranslateRequest(), userId, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contains no questions to translate");
    }

    @Test
    @DisplayName("Should retrieve translation job status correctly")
    void shouldGetTranslationStatus() {
        UUID jobId = UUID.randomUUID();
        BatchTranslationJobResponseDto jobDto = BatchTranslationJobResponseDto.builder()
                .id(jobId)
                .paperId(paperId)
                .status("IN_PROGRESS")
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .totalQuestions(10)
                .processedQuestions(4)
                .successfulQuestions(4)
                .progressPercentage(40.0)
                .build();

        when(questionBankClient.getBatchTranslationStatus(jobId, tenantId)).thenReturn(jobDto);

        PaperTranslateResponse response = translationService.getTranslationStatus(paperId, jobId, tenantId);

        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getProgressPercentage()).isEqualTo(40.0);
        assertThat(response.getSuccessfulQuestions()).isEqualTo(4);
    }
}
