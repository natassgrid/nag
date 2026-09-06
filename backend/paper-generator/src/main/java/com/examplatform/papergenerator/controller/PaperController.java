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

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.dto.PaperResponse;
import com.examplatform.papergenerator.dto.PaperSummaryResponse;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.service.ExaminationLookupService;
import com.examplatform.papergenerator.service.PaperApprovalService;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.papergenerator.service.PaperSerializer;
import com.examplatform.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for paper generation endpoints.
 * Supports blueprint-driven paper generation, paper listing, approval, and validation.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 28.1, 28.2, 28.3, 28.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperAssemblyService paperAssemblyService;
    private final PaperSerializer paperSerializer;
    private final PaperApprovalService paperApprovalService;
    private final PaperRepository paperRepository;
    private final QuestionBankClient questionBankClient;
    private final ObjectMapper objectMapper;
    private final ExaminationLookupService examinationLookupService;

    /**
     * Lists generated papers with optional filters and pagination.
     *
     * @param examId optional exam UUID filter
     * @param status optional status filter (DRAFT, APPROVED, ENCRYPTED)
     * @param page   0-based page index
     * @param size   page size
     * @return 200 OK with paginated paper summaries
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<Page<PaperSummaryResponse>> listPapers(
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String tenantId = getEffectiveTenantId();
        log.info("Listing papers: examId={}, status={}, page={}, size={}, tenant={}",
                examId, status, page, size, tenantId);

        Page<Paper> papers = paperRepository.findPapers(tenantId, examId, status, PageRequest.of(page, size));

        Set<UUID> examIds = papers.getContent().stream()
                .map(Paper::getExamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> shiftIds = papers.getContent().stream()
                .map(Paper::getShiftId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> examNames = examinationLookupService.findExamNames(examIds);
        Map<String, String> shiftNames = examinationLookupService.findShiftNames(shiftIds);

        Page<PaperSummaryResponse> response = papers.map(p -> {
            String examName = examNames.get(p.getExamId());
            String shiftName = shiftNames.get(p.getShiftId());
            String resolvedName = p.getName();
            if (resolvedName == null || resolvedName.isBlank()) {
                StringBuilder sb = new StringBuilder();
                if (p.isPractice()) {
                    sb.append("Practice - ");
                }
                if (examName != null && !examName.isBlank()) {
                    sb.append(examName);
                } else {
                    sb.append("Exam Paper");
                }
                if (shiftName != null && !shiftName.isBlank()) {
                    sb.append(" (").append(shiftName).append(")");
                } else if (p.getShiftId() != null && !p.getShiftId().isBlank()) {
                    sb.append(" [Shift: ").append(p.getShiftId()).append("]");
                }
                resolvedName = sb.toString();
            }

            return PaperSummaryResponse.builder()
                    .paperId(p.getId())
                    .name(resolvedName)
                    .examId(p.getExamId())
                    .examName(examName)
                    .shiftId(p.getShiftId())
                    .shiftName(shiftName)
                    .status(p.getStatus())
                    .isPractice(p.isPractice())
                    .difficultyScore(p.getDifficultyScore())
                    .encryptionKeyId(p.getEncryptionKeyId())
                    .createdAt(p.getCreatedAt())
                    .build();
        });

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed information and summary for a single paper.
     *
     * @param paperId the paper UUID
     * @return 200 OK with paper details and enriched question/topic breakdown
     */
    @GetMapping("/{paperId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<PaperResponse> getPaper(@PathVariable UUID paperId) {
        String tenantId = getEffectiveTenantId();
        log.info("Retrieving paper: paperId={}, tenant={}", paperId, tenantId);

        Paper paper = paperRepository.findByIdAndTenantId(paperId, tenantId)
                .or(() -> paperRepository.findById(paperId))
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        int totalQuestions = 0;
        List<UUID> questionIds = new ArrayList<>();
        if (paper.getPaperDefinitionJson() != null && !paper.getPaperDefinitionJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(paper.getPaperDefinitionJson());
                JsonNode qIdsNode = root.get("questionIds");
                if (qIdsNode != null && qIdsNode.isArray()) {
                    for (JsonNode qNode : qIdsNode) {
                        try {
                            questionIds.add(UUID.fromString(qNode.asText()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    totalQuestions = questionIds.size();
                }
            } catch (Exception e) {
                log.warn("Failed to parse paperDefinitionJson for paper {}: {}", paperId, e.getMessage());
            }
        }

        Map<String, Integer> topicDistribution = new HashMap<>();
        if (paper.getTopicDistributionJson() != null && !paper.getTopicDistributionJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(paper.getTopicDistributionJson());
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    topicDistribution.put(field.getKey(), field.getValue().asInt());
                }
            } catch (Exception e) {
                log.warn("Failed to parse topicDistributionJson for paper {}: {}", paperId, e.getMessage());
            }
        }

        List<QuestionSummary> questions = Collections.emptyList();
        if (!questionIds.isEmpty()) {
            try {
                questions = questionBankClient.findQuestionsByIds(questionIds, tenantId);
            } catch (Exception e) {
                log.warn("Failed to fetch question summaries for paper {}: {}", paperId, e.getMessage());
            }
        }

        Map<UUID, String> examNames = paper.getExamId() != null
                ? examinationLookupService.findExamNames(Set.of(paper.getExamId()))
                : Collections.emptyMap();
        Map<String, String> shiftNames = paper.getShiftId() != null
                ? examinationLookupService.findShiftNames(Set.of(paper.getShiftId()))
                : Collections.emptyMap();

        String examName = examNames.get(paper.getExamId());
        String shiftName = shiftNames.get(paper.getShiftId());
        String resolvedName = paper.getName();
        if (resolvedName == null || resolvedName.isBlank()) {
            StringBuilder sb = new StringBuilder();
            if (paper.isPractice()) {
                sb.append("Practice - ");
            }
            if (examName != null && !examName.isBlank()) {
                sb.append(examName);
            } else {
                sb.append("Exam Paper");
            }
            if (shiftName != null && !shiftName.isBlank()) {
                sb.append(" (").append(shiftName).append(")");
            } else if (paper.getShiftId() != null && !paper.getShiftId().isBlank()) {
                sb.append(" [Shift: ").append(paper.getShiftId()).append("]");
            }
            resolvedName = sb.toString();
        }

        PaperResponse response = PaperResponse.builder()
                .id(paper.getId())
                .name(resolvedName)
                .examId(paper.getExamId())
                .examName(examName)
                .shiftId(paper.getShiftId())
                .shiftName(shiftName)
                .status(paper.getStatus())
                .isPractice(paper.isPractice())
                .paperDefinitionJson(paper.getPaperDefinitionJson())
                .difficultyScore(paper.getDifficultyScore())
                .topicDistributionJson(paper.getTopicDistributionJson())
                .encryptedPackageRef(paper.getEncryptedPackageRef())
                .encryptionKeyId(paper.getEncryptionKeyId())
                .generatedBy(paper.getGeneratedBy())
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .totalQuestions(totalQuestions)
                .topicDistribution(topicDistribution)
                .questions(questions)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Submits an async paper generation job.
     * Requires the EXAM_CONTROLLER role.
     *
     * @param request the paper generation request with blueprint rules
     * @param jwt     the authenticated user's JWT
     * @return 202 Accepted with the paper ID and name
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> generatePaper(
            @Valid @RequestBody PaperGenerationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID generatedBy = UUID.fromString(jwt.getSubject());
        String tenantId = getEffectiveTenantId();

        log.info("Paper generation requested by user={}, examId={}, shiftId={}, isPractice={}",
                generatedBy, request.getExamId(), request.getShiftId(), request.getIsPractice());

        Paper paper = paperAssemblyService.generatePaper(request, generatedBy, tenantId);

        Map<String, Object> response = Map.of(
                "paperId", paper.getId(),
                "name", paper.getName() != null ? paper.getName() : "",
                "status", paper.getStatus(),
                "isPractice", paper.isPractice(),
                "message", "Paper generation submitted successfully"
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Validates a paper JSON document against the expected schema.
     * Returns 200 OK if valid, or 422 Unprocessable Entity with validation errors.
     *
     * @param json the paper JSON string to validate
     * @return 200 OK or 422 with validation error details
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> validatePaper(@RequestBody String json) {
        log.info("Paper schema validation requested");

        List<String> errors = paperSerializer.validate(json);

        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Paper document is valid"
            ));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "valid", false,
                "errors", errors
        ));
    }

    /**
     * Approves a paper, encrypts it with a shift-specific key, and transitions
     * through DRAFT → APPROVED → ENCRYPTED.
     *
     * @param paperId the paper ID to approve
     * @return 200 OK with the updated paper details
     */
    @PostMapping("/{paperId}/approve")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> approvePaper(@PathVariable UUID paperId) {
        String tenantId = getEffectiveTenantId();
        log.info("Paper approval requested for paperId={}", paperId);

        Paper paper = paperApprovalService.approvePaper(paperId, tenantId);

        return ResponseEntity.ok(Map.of(
                "paperId", paper.getId(),
                "name", paper.getName() != null ? paper.getName() : "",
                "status", paper.getStatus(),
                "isPractice", paper.isPractice(),
                "encryptionKeyId", paper.getEncryptionKeyId() != null ? paper.getEncryptionKeyId() : "",
                "message", "Paper approved and encrypted successfully"
        ));
    }

    private String getEffectiveTenantId() {
        String tenantId = TenantContext.get();
        return (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
    }
}
