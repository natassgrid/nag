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

package com.examplatform.asset.controller;

import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import com.examplatform.asset.domain.enums.ReferenceType;
import com.examplatform.asset.dto.AssetMetadataUpdateRequest;
import com.examplatform.asset.dto.AssetReferenceRequest;
import com.examplatform.asset.dto.AssetReferenceResponse;
import com.examplatform.asset.dto.AssetUploadResponse;
import com.examplatform.asset.service.AssetService;
import com.examplatform.asset.service.ReferenceService;
import com.examplatform.asset.support.AbstractIntegrationTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AssetController REST Endpoints E2E Tests (MockMvc)")
class AssetRestControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private ReferenceService referenceService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ASSET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("POST /api/v1/assets (Upload)")
    class UploadEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR uploads file - returns 201 Created")
        void authorCanUploadAsset() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.png", "image/png", new byte[]{1, 2, 3});

            AssetUploadResponse response = AssetUploadResponse.builder()
                    .id(ASSET_ID)
                    .originalFilename("diagram.png")
                    .contentType("image/png")
                    .assetType(AssetType.IMAGE)
                    .status(AssetStatus.ACTIVE)
                    .fileSize(3L)
                    .build();

            when(assetService.upload(any(), any(), anyString())).thenReturn(response);

            mockMvc.perform(multipart("/api/v1/assets")
                            .file(file)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(ASSET_ID.toString()))
                    .andExpect(jsonPath("$.data.originalFilename").value("diagram.png"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbidden() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/v1/assets")
                            .file(file)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/v1/assets")
                            .file(file)
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/assets/{id}")
    class GetAssetEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves asset - returns 200 OK")
        void candidateCanGetAsset() throws Exception {
            AssetUploadResponse response = AssetUploadResponse.builder()
                    .id(ASSET_ID)
                    .originalFilename("photo.jpg")
                    .contentType("image/jpeg")
                    .status(AssetStatus.ACTIVE)
                    .build();

            when(assetService.getAsset(eq(ASSET_ID))).thenReturn(response);

            mockMvc.perform(get("/api/v1/assets/{id}", ASSET_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(ASSET_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Nonexistent asset returns 404 Not Found")
        void notFoundReturns404() throws Exception {
            when(assetService.getAsset(eq(ASSET_ID)))
                    .thenThrow(new EntityNotFoundException("Asset not found: " + ASSET_ID));

            mockMvc.perform(get("/api/v1/assets/{id}", ASSET_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/assets/{id}", ASSET_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/assets (List)")
    class ListAssetsEndpoint {

        @Test
        @DisplayName("+ve: ADMIN lists assets - returns 200 OK")
        void adminCanListAssets() throws Exception {
            when(assetService.listAssets(anyInt(), anyInt(), anyString()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/assets")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromList() throws Exception {
            mockMvc.perform(get("/api/v1/assets")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/assets/{id}/metadata")
    class UpdateMetadataEndpoint {

        @Test
        @DisplayName("+ve: CONTENT_MANAGER updates metadata - returns 200 OK")
        void contentManagerCanUpdateMetadata() throws Exception {
            AssetMetadataUpdateRequest request = AssetMetadataUpdateRequest.builder()
                    .title("Updated Title")
                    .description("Updated Description")
                    .build();

            AssetUploadResponse response = AssetUploadResponse.builder()
                    .id(ASSET_ID)
                    .title("Updated Title")
                    .description("Updated Description")
                    .build();

            when(assetService.updateMetadata(eq(ASSET_ID), any(), any(), anyString()))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/assets/{id}/metadata", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CONTENT_MANAGER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Updated Title"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            AssetMetadataUpdateRequest request = AssetMetadataUpdateRequest.builder().title("Title").build();

            mockMvc.perform(put("/api/v1/assets/{id}/metadata", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/assets/{id}")
    class DeleteAssetEndpoint {

        @Test
        @DisplayName("+ve: ADMIN deletes asset - returns 200 OK")
        void adminCanDeleteAsset() throws Exception {
            doNothing().when(assetService).deleteAsset(eq(ASSET_ID), any(), anyString());

            mockMvc.perform(delete("/api/v1/assets/{id}", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_QUESTION_AUTHOR) returns 403 Forbidden")
        void questionAuthorForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/assets/{id}", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/assets/{id}/archive & restore")
    class ArchiveAndRestoreEndpoints {

        @Test
        @DisplayName("+ve: ADMIN archives asset - returns 200 OK")
        void adminCanArchiveAsset() throws Exception {
            AssetUploadResponse response = AssetUploadResponse.builder()
                    .id(ASSET_ID)
                    .status(AssetStatus.ARCHIVED)
                    .build();

            when(assetService.archiveAsset(eq(ASSET_ID), any(), anyString())).thenReturn(response);

            mockMvc.perform(put("/api/v1/assets/{id}/archive", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        }

        @Test
        @DisplayName("+ve: ADMIN restores asset - returns 200 OK")
        void adminCanRestoreAsset() throws Exception {
            AssetUploadResponse response = AssetUploadResponse.builder()
                    .id(ASSET_ID)
                    .status(AssetStatus.ACTIVE)
                    .build();

            when(assetService.restoreAsset(eq(ASSET_ID), any(), anyString())).thenReturn(response);

            mockMvc.perform(put("/api/v1/assets/{id}/restore", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(put("/api/v1/assets/{id}/archive", ASSET_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Reference Management Endpoints")
    class ReferenceManagementEndpoints {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR adds reference - returns 201 Created")
        void authorCanAddReference() throws Exception {
            UUID refId = UUID.randomUUID();
            AssetReferenceRequest request = AssetReferenceRequest.builder()
                    .assetId(ASSET_ID)
                    .referenceType(ReferenceType.QUESTION)
                    .referenceId(refId)
                    .build();

            AssetReferenceResponse response = AssetReferenceResponse.builder()
                    .id(UUID.randomUUID())
                    .assetId(ASSET_ID)
                    .referenceType(ReferenceType.QUESTION)
                    .referenceId(refId)
                    .createdBy(USER_ID)
                    .createdAt(Instant.now())
                    .build();

            when(referenceService.addReference(any(), any(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/v1/assets/references")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.assetId").value(ASSET_ID.toString()));
        }

        @Test
        @DisplayName("+ve: REVIEWER lists references - returns 200 OK")
        void reviewerCanListReferences() throws Exception {
            when(referenceService.getReferencesForAsset(eq(ASSET_ID))).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/assets/{id}/references", ASSET_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("+ve: ADMIN removes reference - returns 200 OK")
        void adminCanRemoveReference() throws Exception {
            UUID refId = UUID.randomUUID();
            doNothing().when(referenceService).removeReference(eq(refId));

            mockMvc.perform(delete("/api/v1/assets/references/{refId}", refId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            UUID refId = UUID.randomUUID();
            AssetReferenceRequest request = AssetReferenceRequest.builder()
                    .assetId(ASSET_ID)
                    .referenceType(ReferenceType.QUESTION)
                    .referenceId(refId)
                    .build();

            mockMvc.perform(post("/api/v1/assets/references")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
