package com.examplatform.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing the full Subject → Topic → Subtopic hierarchy tree.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectHierarchyResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private boolean active;
    private List<TopicNode> topics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicNode {
        private UUID id;
        private String name;
        private String description;
        private boolean active;
        private List<SubtopicNode> subtopics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtopicNode {
        private UUID id;
        private String name;
        private String description;
        private boolean active;
    }
}
