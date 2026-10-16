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

package com.examplatform.questionbank.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Prompt Template Service utilizing Spring AI StringTemplate (ST) mechanism.
 *
 * Features:
 * 1. Externalized classpath resource loading (prompts ST files, YAML bundles, JSON).
 * 2. YAML template bundle resolution (hierarchical dot-notation keys like microlesson.system).
 * 3. Dynamic parameter substitution with type-safe Map context.
 * 4. Prompt versioning and environment-specific overrides (prompts/version/...).
 * 5. In-memory compiled template caching with thread-safe invalidation.
 */
@Slf4j
@Service
public class PromptTemplateService {

    private final ResourceLoader resourceLoader;
    private final String promptsDirectory;
    private final String activeVersion;
    private final Map<String, PromptTemplate> templateCache = new ConcurrentHashMap<>();
    private final Map<String, String> yamlPromptsCache = new ConcurrentHashMap<>();

    public PromptTemplateService(
            ResourceLoader resourceLoader,
            @Value("${app.ai.prompts.dir:classpath:prompts/}") String promptsDirectory,
            @Value("${app.ai.prompts.version:v1}") String activeVersion) {
        this.resourceLoader = resourceLoader;
        this.promptsDirectory = promptsDirectory.endsWith("/") ? promptsDirectory : promptsDirectory + "/";
        this.activeVersion = activeVersion;
        loadYamlPromptsBundle();
    }

    /**
     * Renders a prompt template with parameter substitution.
     *
     * @param promptKeyOrPath key in YAML bundle (e.g. "microlesson.user") or file path relative to prompts dir
     * @param variables Map of template placeholder names to replacement values
     * @return fully rendered prompt string
     */
    public String render(String promptKeyOrPath, Map<String, Object> variables) {
        PromptTemplate template = getOrLoadTemplate(promptKeyOrPath);
        Map<String, Object> model = (variables != null) ? new HashMap<>(variables) : new HashMap<>();

        try {
            return template.render(model);
        } catch (Exception e) {
            log.warn("Spring AI ST render exception for '{}': {}. Falling back to token replacement.", promptKeyOrPath, e.getMessage());
            return fallbackRender(template.getTemplate(), model);
        }
    }

    /**
     * Renders a prompt directly from a Spring Resource with parameter substitution.
     */
    public String render(Resource resource, Map<String, Object> variables) {
        PromptTemplate template = new PromptTemplate(resource);
        Map<String, Object> model = (variables != null) ? new HashMap<>(variables) : new HashMap<>();
        try {
            return template.render(model);
        } catch (Exception e) {
            log.warn("Resource template render exception: {}. Falling back to token replacement.", e.getMessage());
            return fallbackRender(template.getTemplate(), model);
        }
    }

    /**
     * Retrieves static prompt without parameter substitution.
     */
    public String getPrompt(String promptKeyOrPath) {
        return render(promptKeyOrPath, Collections.emptyMap());
    }

    /**
     * Obtains or compiles the PromptTemplate for the specified key or resource path.
     */
    public PromptTemplate getTemplate(String promptKeyOrPath) {
        return getOrLoadTemplate(promptKeyOrPath);
    }

    /**
     * Clears cached compiled templates to force reload.
     */
    public void clearCache() {
        templateCache.clear();
        yamlPromptsCache.clear();
        loadYamlPromptsBundle();
    }

    private PromptTemplate getOrLoadTemplate(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            throw new IllegalArgumentException("Prompt key or path cannot be empty");
        }

        return templateCache.computeIfAbsent(keyOrPath, key -> {
            // 1. Check YAML bundle cache (e.g., "microlesson.system")
            if (yamlPromptsCache.containsKey(key)) {
                return new PromptTemplate(yamlPromptsCache.get(key));
            }
            String altKey = key.replace('/', '.');
            if (yamlPromptsCache.containsKey(altKey)) {
                return new PromptTemplate(yamlPromptsCache.get(altKey));
            }

            // 2. Check versioned override path: prompts/{version}/{key}
            String normalizedPath = key.endsWith(".st") ? key : key + ".st";
            Resource versionedResource = resourceLoader.getResource(promptsDirectory + activeVersion + "/" + normalizedPath);
            if (versionedResource.exists() && versionedResource.isReadable()) {
                log.debug("Loading versioned prompt override from: {}", versionedResource.getDescription());
                return new PromptTemplate(versionedResource);
            }

            // 3. Check base prompts directory path
            Resource baseResource = resourceLoader.getResource(promptsDirectory + normalizedPath);
            if (baseResource.exists() && baseResource.isReadable()) {
                log.debug("Loading base prompt from: {}", baseResource.getDescription());
                return new PromptTemplate(baseResource);
            }

            // 4. Check absolute classpath location if provided
            if (key.startsWith("classpath:") || key.startsWith("file:")) {
                Resource directResource = resourceLoader.getResource(key);
                if (directResource.exists() && directResource.isReadable()) {
                    return new PromptTemplate(directResource);
                }
            }

            throw new NoSuchElementException("Prompt template not found for key/path: " + key);
        });
    }

    private void loadYamlPromptsBundle() {
        try {
            Resource yamlResource = resourceLoader.getResource(promptsDirectory + "prompts.yaml");
            if (!yamlResource.exists()) {
                yamlResource = resourceLoader.getResource(promptsDirectory + "prompts.yml");
            }
            if (!yamlResource.exists()) {
                return;
            }

            Yaml yaml = new Yaml();
            try (InputStream is = yamlResource.getInputStream()) {
                Map<String, Object> data = yaml.load(is);
                if (data != null && data.containsKey("prompts") && data.get("prompts") instanceof Map<?, ?> promptsMap) {
                    flattenPrompts("", (Map<?, ?>) promptsMap, yamlPromptsCache);
                    log.info("Loaded {} prompt templates from YAML bundle", yamlPromptsCache.size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load YAML prompt bundle: {}", e.getMessage());
        }
    }

    private void flattenPrompts(String prefix, Map<?, ?> source, Map<String, String> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) {
                flattenPrompts(key, nested, target);
            } else if (entry.getValue() != null) {
                target.put(key, String.valueOf(entry.getValue()).trim());
            }
        }
    }

    private String fallbackRender(String rawTemplate, Map<String, Object> variables) {
        if (rawTemplate == null) return "";
        String result = rawTemplate;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }
}
