// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.exception.PracticeSetNotFoundException;
import com.examplatform.practice.repository.PracticeSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeSetServiceTest {

    @Mock
    private PracticeSetRepository practiceSetRepository;

    @InjectMocks
    private PracticeSetService practiceSetService;

    private UUID setId;
    private PracticeSet practiceSet;

    @BeforeEach
    void setUp() {
        setId = UUID.randomUUID();
        practiceSet = PracticeSet.builder()
                .name("Math Practice Blueprint")
                .description("Mock math set")
                .durationMinutes(60)
                .totalQuestions(25)
                .source("EXAM_CLONE")
                .subjectSlug("math")
                .published(true)
                .questionIds("[\"" + UUID.randomUUID() + "\"]")
                .createdBy(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(practiceSet, "id", setId);
    }

    @Test
    @DisplayName("getPublishedSets - returns only published sets as DTOs")
    void testGetPublishedSets() {
        when(practiceSetRepository.findByPublishedTrue()).thenReturn(List.of(practiceSet));

        List<PracticeSetDto> dtos = practiceSetService.getPublishedSets();

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).name()).isEqualTo("Math Practice Blueprint");
        assertThat(dtos.get(0).source()).isEqualTo("EXAM_CLONE");
        assertThat(dtos.get(0).totalQuestions()).isEqualTo(25);
        verify(practiceSetRepository).findByPublishedTrue();
    }

    @Test
    @DisplayName("getSet - returns set DTO when set exists and is published")
    void testGetSetSuccess() {
        when(practiceSetRepository.findById(setId)).thenReturn(Optional.of(practiceSet));

        PracticeSetDto dto = practiceSetService.getSet(setId);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(setId);
        assertThat(dto.name()).isEqualTo("Math Practice Blueprint");
    }

    @Test
    @DisplayName("getSet - throws PracticeSetNotFoundException when set does not exist or unpublished")
    void testGetSetNotFound() {
        practiceSet.setPublished(false);
        when(practiceSetRepository.findById(setId)).thenReturn(Optional.of(practiceSet));

        assertThatThrownBy(() -> practiceSetService.getSet(setId))
                .isInstanceOf(PracticeSetNotFoundException.class);
    }
}
