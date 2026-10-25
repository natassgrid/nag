/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectTopicService Unit Tests")
class SubjectTopicServiceTest {

    private static final String TENANT_ID = "default";

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubtopicRepository subtopicRepository;

    private SubjectTopicService service;

    private Subject mockSubject;
    private Topic mockTopic;
    private Subtopic mockSubtopic;

    @BeforeEach
    void setUp() {
        service = new SubjectTopicService(subjectRepository, topicRepository, subtopicRepository);

        mockSubject = Subject.builder().name("Mathematics").code("MATH").build();
        ReflectionTestUtils.setField(mockSubject, "id", 10L);

        mockTopic = Topic.builder().name("Calculus").code("CALC").subjectId(10L).build();
        ReflectionTestUtils.setField(mockTopic, "id", 194L);

        mockSubtopic = Subtopic.builder().name("Differential Calculus").code("DIFF").topicId(194L).build();
        ReflectionTestUtils.setField(mockSubtopic, "id", 500L);
    }

    @Test
    @DisplayName("resolveByIds: returns exact subtopic when subtopicId is provided")
    void resolveByIdsWithExplicitSubtopic() {
        when(subjectRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(mockSubject));
        when(topicRepository.findByIdAndSubjectIdAndTenantId(194L, 10L, TENANT_ID)).thenReturn(Optional.of(mockTopic));
        when(subtopicRepository.findByIdAndTopicIdAndTenantId(500L, 194L, TENANT_ID)).thenReturn(Optional.of(mockSubtopic));

        var result = service.resolveByIds(10L, 194L, 500L, TENANT_ID);

        assertThat(result.subjectId()).isEqualTo(10L);
        assertThat(result.topicId()).isEqualTo(194L);
        assertThat(result.subtopicId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("resolveByIds: picks first existing subtopic when subtopicId is null")
    void resolveByIdsWithNullSubtopicPicksFirstExisting() {
        when(subjectRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(mockSubject));
        when(topicRepository.findByIdAndSubjectIdAndTenantId(194L, 10L, TENANT_ID)).thenReturn(Optional.of(mockTopic));
        when(subtopicRepository.findByTopicIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(194L))
                .thenReturn(Collections.emptyList());
        when(subtopicRepository.findByTopicIdAndTenantIdOrderByDisplayOrderAscNameAsc(194L, TENANT_ID))
                .thenReturn(List.of(mockSubtopic));

        var result = service.resolveByIds(10L, 194L, null, TENANT_ID);

        assertThat(result.subjectId()).isEqualTo(10L);
        assertThat(result.topicId()).isEqualTo(194L);
        assertThat(result.subtopicId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("resolveByIds: auto-creates default General subtopic when topic has no subtopics")
    void resolveByIdsWithNullSubtopicCreatesDefaultWhenNoSubtopicsExist() {
        when(subjectRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(mockSubject));
        when(topicRepository.findByIdAndSubjectIdAndTenantId(194L, 10L, TENANT_ID)).thenReturn(Optional.of(mockTopic));
        when(subtopicRepository.findByTopicIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(194L))
                .thenReturn(Collections.emptyList());
        when(subtopicRepository.findByTopicIdAndTenantIdOrderByDisplayOrderAscNameAsc(194L, TENANT_ID))
                .thenReturn(Collections.emptyList());

        Subtopic created = Subtopic.builder().name("General Calculus").topicId(194L).build();
        ReflectionTestUtils.setField(created, "id", 999L);
        when(subtopicRepository.existsByNameAndTopicIdAndTenantId("General Calculus", 194L, TENANT_ID)).thenReturn(false);
        when(subtopicRepository.existsByNameAndTopicIdAndTenantIdIsNull("General Calculus", 194L)).thenReturn(false);
        when(subtopicRepository.save(any(Subtopic.class))).thenReturn(created);

        var result = service.resolveByIds(10L, 194L, null, TENANT_ID);

        assertThat(result.subjectId()).isEqualTo(10L);
        assertThat(result.topicId()).isEqualTo(194L);
        assertThat(result.subtopicId()).isEqualTo(999L);
        verify(subtopicRepository).save(argThat(st -> "General Calculus".equals(st.getName())));
    }

    @Test
    @DisplayName("resolveByIds: throws IllegalArgumentException when subject is unknown")
    void resolveByIdsThrowsWhenSubjectNotFound() {
        when(subjectRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());
        when(subjectRepository.findByIdAndTenantIdIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveByIds(99L, 194L, null, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subject not found: id=99");
    }

    @Test
    @DisplayName("resolveByIds: throws IllegalArgumentException when topic is unknown")
    void resolveByIdsThrowsWhenTopicNotFound() {
        when(subjectRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(mockSubject));
        when(topicRepository.findByIdAndSubjectIdAndTenantId(999L, 10L, TENANT_ID)).thenReturn(Optional.empty());
        when(topicRepository.findByIdAndSubjectIdAndTenantIdIsNull(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveByIds(10L, 999L, null, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Topic not found: id=999");
    }
}
