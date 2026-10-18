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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Blueprint;
import com.examplatform.papergenerator.domain.BlueprintSection;
import com.examplatform.papergenerator.domain.BlueprintRule;
import com.examplatform.papergenerator.dto.*;
import com.examplatform.papergenerator.repository.BlueprintRepository;
import com.examplatform.papergenerator.repository.BlueprintRuleRepository;
import com.examplatform.papergenerator.repository.BlueprintSectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for relational blueprint lifecycle management and sufficiency evaluation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BlueprintService {

    private final BlueprintRepository blueprintRepository;
    private final BlueprintSectionRepository blueprintSectionRepository;
    private final BlueprintRuleRepository blueprintRuleRepository;
    private final PaperAssemblyService paperAssemblyService;

    public BlueprintResponse create(BlueprintRequest request, UUID createdBy, String tenantId) {
        if (blueprintRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new IllegalArgumentException(
                    "A blueprint named '" + request.getName() + "' already exists");
        }

        Blueprint blueprint = Blueprint.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .blueprintVersion(1)
                .status("ACTIVE")
                .createdBy(createdBy)
                .build();
        blueprint.setTenantId(tenantId);

        // Calculate totals
        int totalQ = 0;
        double totalM = 0.0;

        List<BlueprintSection> sections = new ArrayList<>();
        int secIdx = 0;

        if (request.getSections() != null && !request.getSections().isEmpty()) {
            for (BlueprintSectionDto sDto : request.getSections()) {
                BlueprintSection sec = BlueprintSection.builder()
                        .blueprint(blueprint)
                        .name(sDto.getName())
                        .orderIndex(sDto.getOrderIndex() != 0 ? sDto.getOrderIndex() : secIdx++)
                        .subjectId(sDto.getSubjectId() != null ? sDto.getSubjectId() : 1L)
                        .rules(new ArrayList<>())
                        .build();
                sec.setTenantId(tenantId);

                if (sDto.getRules() != null) {
                    int rIdx = 0;
                    for (com.examplatform.papergenerator.dto.BlueprintRule rDto : sDto.getRules()) {
                        int qc = rDto.getQuestionCount();
                        totalQ += qc;
                        double marks = rDto.getMarksOverride() != null ? rDto.getMarksOverride() : 1.0;
                        totalM += (qc * marks);

                        BlueprintRule rule = BlueprintRule.builder()
                                .section(sec)
                                .subjectId(rDto.getSubjectId() != null ? rDto.getSubjectId() : sec.getSubjectId())
                                .topicId(rDto.getTopicId())
                                .subtopicId(rDto.getSubtopicId())
                                .difficulty(rDto.getDifficulty())
                                .cognitiveLevel(rDto.getCognitiveLevel())
                                .questionCount(qc)
                                .marksOverride(rDto.getMarksOverride())
                                .orderIndex(rDto.getOrderIndex() != 0 ? rDto.getOrderIndex() : rIdx++)
                                .build();
                        rule.setTenantId(tenantId);
                        sec.addRule(rule);
                    }
                }
                sections.add(sec);
            }
        } else if (request.getRules() != null && !request.getRules().isEmpty()) {
            // Backward compatibility: flat list of rules grouped into one section
            BlueprintSection defaultSec = BlueprintSection.builder()
                    .blueprint(blueprint)
                    .name("Default Section")
                    .orderIndex(0)
                    .subjectId(1L)
                    .rules(new ArrayList<>())
                    .build();
            defaultSec.setTenantId(tenantId);

            int rIdx = 0;
            for (com.examplatform.papergenerator.dto.BlueprintRule rDto : request.getRules()) {
                int qc = rDto.getQuestionCount();
                totalQ += qc;
                double marks = rDto.getMarksOverride() != null ? rDto.getMarksOverride() : 1.0;
                totalM += (qc * marks);

                BlueprintRule rule = BlueprintRule.builder()
                        .section(defaultSec)
                        .subjectId(rDto.getSubjectId() != null ? rDto.getSubjectId() : 1L)
                        .topicId(rDto.getTopicId())
                        .subtopicId(rDto.getSubtopicId())
                        .difficulty(rDto.getDifficulty())
                        .cognitiveLevel(rDto.getCognitiveLevel())
                        .questionCount(qc)
                        .marksOverride(rDto.getMarksOverride())
                        .orderIndex(rDto.getOrderIndex() != 0 ? rDto.getOrderIndex() : rIdx++)
                        .build();
                rule.setTenantId(tenantId);
                defaultSec.addRule(rule);
            }
            sections.add(defaultSec);
        }

        blueprint.setTotalQuestions(totalQ);
        blueprint.setTotalMarks(totalM);
        blueprint.setSections(sections);

        Blueprint saved = blueprintRepository.save(blueprint);
        log.info("Blueprint created: id={}, name='{}', tenant={}", saved.getId(), saved.getName(), tenantId);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BlueprintResponse> listVisible(String tenantId) {
        return blueprintRepository.findVisibleBlueprints(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlueprintResponse getById(UUID id, String tenantId) {
        Blueprint blueprint = blueprintRepository.findVisibleById(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Blueprint not found: " + id));
        return toResponse(blueprint);
    }

    public BlueprintResponse update(UUID id, BlueprintRequest request, String tenantId) {
        Blueprint blueprint = blueprintRepository.findVisibleById(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Blueprint not found: " + id));

        if (blueprint.getTenantId() != null && !blueprint.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Cannot modify blueprint belonging to another tenant");
        }

        if (!blueprint.getName().equals(request.getName())
                && blueprintRepository.existsByNameAndTenantIdAndIdNot(request.getName(), tenantId, id)) {
            throw new IllegalArgumentException(
                    "A blueprint named '" + request.getName() + "' already exists");
        }

        blueprint.setName(request.getName());
        blueprint.setDescription(request.getDescription());
        if (request.getCode() != null) {
            blueprint.setCode(request.getCode());
        }

        blueprintSectionRepository.deleteByBlueprintId(id);

        List<BlueprintSection> newSections = new ArrayList<>();
        int totalQ = 0;
        double totalM = 0.0;
        int secIdx = 0;

        if (request.getSections() != null && !request.getSections().isEmpty()) {
            for (BlueprintSectionDto sDto : request.getSections()) {
                BlueprintSection sec = BlueprintSection.builder()
                        .blueprint(blueprint)
                        .name(sDto.getName())
                        .orderIndex(sDto.getOrderIndex() != 0 ? sDto.getOrderIndex() : secIdx++)
                        .subjectId(sDto.getSubjectId() != null ? sDto.getSubjectId() : 1L)
                        .rules(new ArrayList<>())
                        .build();
                sec.setTenantId(tenantId);

                if (sDto.getRules() != null) {
                    int rIdx = 0;
                    for (com.examplatform.papergenerator.dto.BlueprintRule rDto : sDto.getRules()) {
                        int qc = rDto.getQuestionCount();
                        totalQ += qc;
                        double marks = rDto.getMarksOverride() != null ? rDto.getMarksOverride() : 1.0;
                        totalM += (qc * marks);

                        BlueprintRule rule = BlueprintRule.builder()
                                .section(sec)
                                .subjectId(rDto.getSubjectId() != null ? rDto.getSubjectId() : sec.getSubjectId())
                                .topicId(rDto.getTopicId())
                                .subtopicId(rDto.getSubtopicId())
                                .difficulty(rDto.getDifficulty())
                                .cognitiveLevel(rDto.getCognitiveLevel())
                                .questionCount(qc)
                                .marksOverride(rDto.getMarksOverride())
                                .orderIndex(rDto.getOrderIndex() != 0 ? rDto.getOrderIndex() : rIdx++)
                                .build();
                        rule.setTenantId(tenantId);
                        sec.addRule(rule);
                    }
                }
                newSections.add(sec);
            }
        }

        blueprint.setTotalQuestions(totalQ);
        blueprint.setTotalMarks(totalM);
        blueprint.setSections(newSections);

        Blueprint saved = blueprintRepository.save(blueprint);
        log.info("Blueprint updated: id={}, name='{}', tenant={}", saved.getId(), saved.getName(), tenantId);

        return toResponse(saved);
    }

    public void delete(UUID id, String tenantId) {
        Blueprint blueprint = blueprintRepository.findVisibleById(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Blueprint not found: " + id));

        if (blueprint.getTenantId() == null) {
            throw new IllegalArgumentException("Platform default blueprints cannot be deleted by tenants");
        }

        blueprintRepository.delete(blueprint);
        log.info("Blueprint deleted: id={}, tenant={}", id, tenantId);
    }

    @Transactional(readOnly = true)
    public BlueprintFeasibilityResponse checkSufficiency(
            UUID id,
            UUID examId,
            Integer shifts,
            String tenantId,
            boolean notifyAdmin) {
        return paperAssemblyService.checkBlueprintSufficiencyById(id, examId, shifts, tenantId, notifyAdmin);
    }

    private BlueprintResponse toResponse(Blueprint bp) {
        List<BlueprintSectionDto> sDtos = new ArrayList<>();
        List<com.examplatform.papergenerator.dto.BlueprintRule> flatRules = new ArrayList<>();

        if (bp.getSections() != null) {
            for (BlueprintSection s : bp.getSections()) {
                List<com.examplatform.papergenerator.dto.BlueprintRule> rDtos = new ArrayList<>();
                if (s.getRules() != null) {
                    for (BlueprintRule r : s.getRules()) {
                        com.examplatform.papergenerator.dto.BlueprintRule rDto =
                                com.examplatform.papergenerator.dto.BlueprintRule.builder()
                                        .id(r.getId())
                                        .subjectId(r.getSubjectId())
                                        .topicId(r.getTopicId())
                                        .subtopicId(r.getSubtopicId())
                                        .subject(s.getName())
                                        .difficulty(r.getDifficulty())
                                        .cognitiveLevel(r.getCognitiveLevel())
                                        .questionCount(r.getQuestionCount())
                                        .marksOverride(r.getMarksOverride())
                                        .orderIndex(r.getOrderIndex())
                                        .build();
                        rDtos.add(rDto);
                        flatRules.add(rDto);
                    }
                }
                sDtos.add(BlueprintSectionDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .orderIndex(s.getOrderIndex())
                        .subjectId(s.getSubjectId())
                        .rules(rDtos)
                        .build());
            }
        }

        return BlueprintResponse.builder()
                .id(bp.getId())
                .code(bp.getCode())
                .name(bp.getName())
                .description(bp.getDescription())
                .version(bp.getBlueprintVersion())
                .status(bp.getStatus())
                .totalQuestions(bp.getTotalQuestions())
                .totalMarks(bp.getTotalMarks())
                .platformDefault(bp.getTenantId() == null)
                .sections(sDtos)
                .rules(flatRules)
                .createdBy(bp.getCreatedBy())
                .createdAt(bp.getCreatedAt())
                .updatedAt(bp.getUpdatedAt())
                .build();
    }
}
