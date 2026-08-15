package com.examplatform.asset.controller;

import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import com.examplatform.asset.repository.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.examplatform.asset.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link AssetController}.
 * Uses TestContainers for PostgreSQL and Kafka.
 * Tests the full request lifecycle through the Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("AssetController Integration Tests")
@Disabled
class AssetControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("exam_platform_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.schemas", () -> "asset_service");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaAssetRepository assetRepository;

    @BeforeEach
    void setUp() {
        assetRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/assets")
    class UploadAsset {

        @Test
        @WithMockUser(roles = "QUESTION_AUTHOR")
        @DisplayName("uploads PNG file and returns 201 with metadata")
        void uploadPngReturns201() throws Exception {
            // PNG magic bytes header
            byte[] pngContent = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                    0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test-image.png", "image/png", pngContent);

            mockMvc.perform(multipart("/api/v1/assets")
                            .file(file)
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("success")))
                    .andExpect(jsonPath("$.data.originalFilename", is("test-image.png")))
                    .andExpect(jsonPath("$.data.assetType", is("IMAGE")))
                    .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.data.sha256Hash", notNullValue()))
                    .andExpect(jsonPath("$.data.storageProvider", is("filesystem")))
                    .andExpect(jsonPath("$.data.id", notNullValue()));

            // Verify persisted in DB
            assertThat(assetRepository.count()).isEqualTo(1);
        }

        @Test
        @WithMockUser(roles = "QUESTION_AUTHOR")
        @DisplayName("rejects unsupported MIME type with 400")
        void rejectsUnsupportedMimeType() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "script.sh", "application/x-sh", "#!/bin/bash".getBytes());

            mockMvc.perform(multipart("/api/v1/assets")
                            .file(file)
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is("error")));
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void rejectsUnauthenticated() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/v1/assets")
                            .file(file)
                            .header("X-Tenant-Id", "tenant-1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/assets/{id}")
    class GetAsset {

        @Test
        @WithMockUser(roles = "QUESTION_AUTHOR")
        @DisplayName("returns asset by ID")
        void returnsAssetById() throws Exception {
            // Create an asset directly in the DB
            MediaAsset asset = createTestAsset("tenant-integration");

            mockMvc.perform(get("/api/v1/assets/{id}", asset.getId())
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("success")))
                    .andExpect(jsonPath("$.data.id", is(asset.getId().toString())))
                    .andExpect(jsonPath("$.data.originalFilename", is("test-asset.png")));
        }

        @Test
        @WithMockUser(roles = "QUESTION_AUTHOR")
        @DisplayName("returns 404 for non-existent asset")
        void returns404ForMissing() throws Exception {
            UUID fakeId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/assets/{id}", fakeId)
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is("error")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/assets")
    class ListAssets {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("lists assets with pagination")
        void listsAssetsWithPagination() throws Exception {
            createTestAsset("tenant-list");
            createTestAsset("tenant-list");

            mockMvc.perform(get("/api/v1/assets")
                            .param("page", "0")
                            .param("size", "10")
                            .header("X-Tenant-Id", "tenant-list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("success")))
                    .andExpect(jsonPath("$.data.totalElements", is(2)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/assets/{id}/metadata")
    class UpdateMetadata {

        @Test
        @WithMockUser(roles = "CONTENT_MANAGER")
        @DisplayName("updates metadata fields")
        void updatesMetadata() throws Exception {
            MediaAsset asset = createTestAsset("tenant-integration");

            String requestBody = """
                    {
                        "title": "Updated Title",
                        "description": "Updated description",
                        "altText": "Accessibility text",
                        "tags": "math,geometry",
                        "language": "hi"
                    }
                    """;

            mockMvc.perform(put("/api/v1/assets/{id}/metadata", asset.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Updated Title")))
                    .andExpect(jsonPath("$.data.description", is("Updated description")))
                    .andExpect(jsonPath("$.data.altText", is("Accessibility text")))
                    .andExpect(jsonPath("$.data.tags", is("math,geometry")))
                    .andExpect(jsonPath("$.data.language", is("hi")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/assets/{id}")
    class DeleteAsset {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("soft-deletes unreferenced asset")
        void softDeletesAsset() throws Exception {
            MediaAsset asset = createTestAsset("tenant-integration");

            mockMvc.perform(delete("/api/v1/assets/{id}", asset.getId())
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("success")));

            // Verify status changed
            MediaAsset deleted = assetRepository.findById(asset.getId()).orElseThrow();
            assertThat(deleted.getStatus()).isEqualTo(AssetStatus.DELETED);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/assets/{id}/archive")
    class ArchiveAsset {

        @Test
        @WithMockUser(roles = "CONTENT_MANAGER")
        @DisplayName("archives active asset")
        void archivesActiveAsset() throws Exception {
            MediaAsset asset = createTestAsset("tenant-integration");

            mockMvc.perform(put("/api/v1/assets/{id}/archive", asset.getId())
                            .header("X-Tenant-Id", "tenant-integration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("ARCHIVED")));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MediaAsset createTestAsset(String tenantId) {
        MediaAsset asset = MediaAsset.builder()
                .originalFilename("test-asset.png")
                .contentType("image/png")
                .extension("png")
                .fileSize(1024L)
                .sha256Hash(UUID.randomUUID().toString().replace("-", "") +
                        UUID.randomUUID().toString().replace("-", ""))
                .assetType(AssetType.IMAGE)
                .status(AssetStatus.ACTIVE)
                .storageProvider("filesystem")
                .storageLocation("test/" + UUID.randomUUID() + "/test-asset.png")
                .createdBy(UUID.randomUUID())
                .build();
        asset.setTenantId(tenantId);
        return assetRepository.save(asset);
    }
}
