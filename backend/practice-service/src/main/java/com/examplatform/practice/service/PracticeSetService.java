// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.repository.PracticeSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PracticeSetService {

    private final PracticeSetRepository practiceSetRepository;

    @Transactional(readOnly = true)
    public List<PracticeSetDto> getPublishedSets() {
        return practiceSetRepository.findByPublishedTrue().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PracticeSetDto getSet(UUID id) {
        return practiceSetRepository.findById(id)
                .filter(PracticeSet::isPublished)
                .map(this::toDto)
                .orElseThrow(() -> new com.examplatform.practice.exception.PracticeSetNotFoundException(id));
    }

    private PracticeSetDto toDto(PracticeSet ps) {
        return new PracticeSetDto(
                ps.getId(), ps.getName(), ps.getDescription(), ps.getSource(),
                ps.getDurationMinutes(), ps.getSubjectSlug(), ps.isPublished(), ps.getTotalQuestions()
        );
    }
}
