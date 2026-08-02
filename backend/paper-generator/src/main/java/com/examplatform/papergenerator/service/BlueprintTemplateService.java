package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.BlueprintTemplate;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.BlueprintTemplateRequest;
import com.examplatform.papergenerator.dto.BlueprintTemplateResponse;
import com.examplatform.papergenerator.repository.BlueprintTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD service for named blueprint templates.
 * Rules are stored as JSONB and round-tripped via ObjectMapper.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BlueprintTemplateService {

    private final BlueprintTemplateRepository repository;
    private final ObjectMapper objectMapper;

    // ── Create ────────────────────────────────────────────────────────────────

    public BlueprintTemplateResponse create(
            BlueprintTemplateRequest request,
            UUID createdBy,
            String tenantId) {

        if (repository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new IllegalArgumentException(
                    "A blueprint template named '" + request.getName() + "' already exists");
        }

        BlueprintTemplate template = BlueprintTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .examId(request.getExamId())
                .rulesJson(toJson(request.getRules()))
                .createdBy(createdBy)
                .build();
        template.setTenantId(tenantId);

        BlueprintTemplate saved = repository.save(template);
        log.info("Blueprint template created: id={}, name='{}', tenant={}",
                saved.getId(), saved.getName(), tenantId);
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BlueprintTemplateResponse> listAll(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BlueprintTemplateResponse> listByExam(UUID examId, String tenantId) {
        return repository.findByExamIdAndTenantIdOrderByCreatedAtDesc(examId, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlueprintTemplateResponse getById(UUID id, String tenantId) {
        return toResponse(findOrThrow(id, tenantId));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public BlueprintTemplateResponse update(
            UUID id,
            BlueprintTemplateRequest request,
            String tenantId) {

        BlueprintTemplate template = findOrThrow(id, tenantId);

        // Check name uniqueness only if it changed
        if (!template.getName().equals(request.getName())
                && repository.existsByNameAndTenantIdAndIdNot(request.getName(), tenantId, id)) {
            throw new IllegalArgumentException(
                    "A blueprint template named '" + request.getName() + "' already exists");
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setExamId(request.getExamId());
        template.setRulesJson(toJson(request.getRules()));

        BlueprintTemplate saved = repository.save(template);
        log.info("Blueprint template updated: id={}, name='{}', tenant={}",
                saved.getId(), saved.getName(), tenantId);
        return toResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(UUID id, String tenantId) {
        BlueprintTemplate template = findOrThrow(id, tenantId);
        repository.delete(template);
        log.info("Blueprint template deleted: id={}, name='{}', tenant={}",
                id, template.getName(), tenantId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BlueprintTemplate findOrThrow(UUID id, String tenantId) {
        BlueprintTemplate template = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Blueprint template not found: " + id));
        if (!tenantId.equals(template.getTenantId())) {
            throw new EntityNotFoundException("Blueprint template not found: " + id);
        }
        return template;
    }

    private BlueprintTemplateResponse toResponse(BlueprintTemplate t) {
        return BlueprintTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .examId(t.getExamId())
                .rules(fromJson(t.getRulesJson()))
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private String toJson(List<BlueprintRule> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise blueprint rules", e);
        }
    }

    private List<BlueprintRule> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<BlueprintRule>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise blueprint rules: {}", e.getMessage());
            return List.of();
        }
    }
}
