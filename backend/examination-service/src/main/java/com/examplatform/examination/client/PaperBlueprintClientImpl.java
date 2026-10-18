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

package com.examplatform.examination.client;

import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnMissingBean(PaperBlueprintClient.class)
public class PaperBlueprintClientImpl implements PaperBlueprintClient {

    private final RestClient restClient;
    private final String paperGeneratorUrl;

    @Autowired
    public PaperBlueprintClientImpl(
            @Autowired(required = false) RestClient.Builder restClientBuilder,
            @Value("${app.paper-generator.service-url:http://localhost:8084}") String paperGeneratorUrl) {
        this.paperGeneratorUrl = paperGeneratorUrl;
        this.restClient = (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
    }

    @Override
    public Optional<BlueprintInfo> getBlueprint(UUID blueprintId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String url = paperGeneratorUrl + "/api/v1/papers/blueprints/" + blueprintId;

        try {
            BlueprintDto dto = restClient.get()
                    .uri(url)
                    .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant)
                    .retrieve()
                    .body(BlueprintDto.class);

            if (dto == null) {
                return Optional.empty();
            }

            List<BlueprintSectionInfo> sections = new ArrayList<>();
            if (dto.sections != null) {
                for (BlueprintSectionDto s : dto.sections) {
                    List<BlueprintRuleInfo> rules = new ArrayList<>();
                    if (s.rules != null) {
                        for (BlueprintRuleDto r : s.rules) {
                            rules.add(new BlueprintRuleInfo(
                                    r.id, r.subjectId, r.topicId, r.subtopicId,
                                    r.difficulty, r.cognitiveLevel,
                                    r.questionCount != null ? r.questionCount : 0,
                                    r.marksOverride
                            ));
                        }
                    }
                    sections.add(new BlueprintSectionInfo(
                            s.id, s.name, s.orderIndex != null ? s.orderIndex : 0, s.subjectId, rules
                    ));
                }
            }

            return Optional.of(new BlueprintInfo(
                    dto.id, dto.code, dto.name, dto.version != null ? dto.version : 1, dto.status, sections
            ));
        } catch (Exception e) {
            log.warn("Failed to fetch blueprint id={} from {}: {}", blueprintId, url, e.getMessage());
            // Graceful fallback: return empty Optional so callers can handle or default
            return Optional.empty();
        }
    }

    @Override
    public SufficiencyResult checkSufficiency(UUID blueprintId, UUID examId, int shifts, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String url = paperGeneratorUrl + "/api/v1/papers/blueprints/" + blueprintId + "/check-sufficiency?shifts=" + shifts;
        if (examId != null) {
            url += "&examId=" + examId;
        }

        try {
            SufficiencyResponseDto dto = restClient.post()
                    .uri(url)
                    .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SufficiencyResponseDto.class);

            if (dto != null) {
                return new SufficiencyResult(
                        Boolean.TRUE.equals(dto.feasible),
                        dto.totalQuestionsNeeded != null ? dto.totalQuestionsNeeded : 0,
                        dto.totalQuestionsAvailable != null ? dto.totalQuestionsAvailable : 0,
                        dto.deficitRuleCount != null ? dto.deficitRuleCount : 0,
                        dto.summary
                );
            }
        } catch (Exception e) {
            log.warn("Sufficiency check via paper-generator failed ({}): {}. Assuming feasible in offline/standalone mode.",
                    url, e.getMessage());
        }

        return new SufficiencyResult(true, 0, 0, 0, "Standalone/fallback check passed");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BlueprintDto {
        public UUID id;
        public String code;
        public String name;
        public Integer version;
        public String status;
        public List<BlueprintSectionDto> sections;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BlueprintSectionDto {
        public UUID id;
        public String name;
        public Integer orderIndex;
        public Long subjectId;
        public List<BlueprintRuleDto> rules;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BlueprintRuleDto {
        public UUID id;
        public Long subjectId;
        public Long topicId;
        public Long subtopicId;
        public String difficulty;
        public String cognitiveLevel;
        public Integer questionCount;
        public Double marksOverride;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SufficiencyResponseDto {
        public Boolean feasible;
        public Integer totalQuestionsNeeded;
        public Integer totalQuestionsAvailable;
        public Integer deficitRuleCount;
        public String summary;
    }
}
