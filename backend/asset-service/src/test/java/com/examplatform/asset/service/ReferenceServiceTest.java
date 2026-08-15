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

package com.examplatform.asset.service;

import com.examplatform.asset.domain.entity.AssetReference;
import com.examplatform.asset.domain.enums.ReferenceType;
import com.examplatform.asset.dto.AssetReferenceRequest;
import com.examplatform.asset.dto.AssetReferenceResponse;
import com.examplatform.asset.repository.AssetReferenceRepository;
import com.examplatform.asset.repository.MediaAssetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReferenceService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReferenceService")
class ReferenceServiceTest {

    @Mock
    private AssetReferenceRepository referenceRepository;

    @Mock
    private MediaAssetRepository assetRepository;

    @InjectMocks
    private ReferenceService referenceService;

    @Captor
    private ArgumentCaptor<AssetReference> referenceCaptor;

    private static final UUID ASSET_ID = UUID.randomUUID();
    private static final UUID REFERENCE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-1";

    private AssetReference buildReference(UUID id) {
        AssetReference ref = AssetReference.builder()
                .assetId(ASSET_ID)
                .referenceType(ReferenceType.QUESTION)
                .referenceId(REFERENCE_ID)
                .createdBy(USER_ID)
                .build();
        try {
            var setIdMethod = ref.getClass().getSuperclass().getDeclaredMethod("setId", UUID.class);
            setIdMethod.setAccessible(true);
            setIdMethod.invoke(ref, id);
            var createdAtField = ref.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(ref, Instant.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ref;
    }

    @Nested
    @DisplayName("addReference")
    class AddReference {

        @Test
        @DisplayName("creates new reference when asset exists and no duplicate")
        void createsNewReference() {
            AssetReferenceRequest request = AssetReferenceRequest.builder()
                    .assetId(ASSET_ID)
                    .referenceType(ReferenceType.QUESTION)
                    .referenceId(REFERENCE_ID)
                    .build();

            when(assetRepository.existsById(ASSET_ID)).thenReturn(true);
            when(referenceRepository.existsByAssetIdAndReferenceTypeAndReferenceId(
                    ASSET_ID, ReferenceType.QUESTION, REFERENCE_ID)).thenReturn(false);
            when(referenceRepository.save(any(AssetReference.class))).thenAnswer(invocation -> {
                AssetReference saved = invocation.getArgument(0);
                try {
                    var setIdMethod = saved.getClass().getSuperclass().getDeclaredMethod("setId", UUID.class);
                    setIdMethod.setAccessible(true);
                    setIdMethod.invoke(saved, UUID.randomUUID());
                    var createdAtField = saved.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(saved, Instant.now());
                } catch (Exception e) {
                    // ignore
                }
                return saved;
            });

            AssetReferenceResponse response = referenceService.addReference(request, USER_ID, TENANT_ID);

            assertThat(response).isNotNull();
            assertThat(response.getAssetId()).isEqualTo(ASSET_ID);
            assertThat(response.getReferenceType()).isEqualTo(ReferenceType.QUESTION);
            assertThat(response.getReferenceId()).isEqualTo(REFERENCE_ID);

            verify(referenceRepository).save(referenceCaptor.capture());
            AssetReference saved = referenceCaptor.getValue();
            assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws when asset does not exist")
        void throwsWhenAssetNotFound() {
            AssetReferenceRequest request = AssetReferenceRequest.builder()
                    .assetId(ASSET_ID)
                    .referenceType(ReferenceType.QUESTION)
                    .referenceId(REFERENCE_ID)
                    .build();

            when(assetRepository.existsById(ASSET_ID)).thenReturn(false);

            assertThatThrownBy(() -> referenceService.addReference(request, USER_ID, TENANT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ASSET_ID.toString());

            verify(referenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns existing reference on duplicate")
        void returnsExistingOnDuplicate() {
            AssetReferenceRequest request = AssetReferenceRequest.builder()
                    .assetId(ASSET_ID)
                    .referenceType(ReferenceType.QUESTION)
                    .referenceId(REFERENCE_ID)
                    .build();

            UUID existingRefId = UUID.randomUUID();
            AssetReference existing = buildReference(existingRefId);

            when(assetRepository.existsById(ASSET_ID)).thenReturn(true);
            when(referenceRepository.existsByAssetIdAndReferenceTypeAndReferenceId(
                    ASSET_ID, ReferenceType.QUESTION, REFERENCE_ID)).thenReturn(true);
            when(referenceRepository.findByAssetIdAndReferenceTypeAndReferenceId(
                    ASSET_ID, ReferenceType.QUESTION, REFERENCE_ID))
                    .thenReturn(Optional.of(existing));

            AssetReferenceResponse response = referenceService.addReference(request, USER_ID, TENANT_ID);

            assertThat(response.getId()).isEqualTo(existingRefId);
            verify(referenceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeReference")
    class RemoveReference {

        @Test
        @DisplayName("deletes existing reference")
        void deletesExistingReference() {
            UUID refId = UUID.randomUUID();
            when(referenceRepository.existsById(refId)).thenReturn(true);

            referenceService.removeReference(refId);

            verify(referenceRepository).deleteById(refId);
        }

        @Test
        @DisplayName("throws when reference not found")
        void throwsWhenNotFound() {
            UUID refId = UUID.randomUUID();
            when(referenceRepository.existsById(refId)).thenReturn(false);

            assertThatThrownBy(() -> referenceService.removeReference(refId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(refId.toString());
        }
    }

    @Nested
    @DisplayName("getReferencesForAsset")
    class GetReferencesForAsset {

        @Test
        @DisplayName("returns all references for an asset")
        void returnsAllReferences() {
            UUID ref1Id = UUID.randomUUID();
            UUID ref2Id = UUID.randomUUID();
            AssetReference ref1 = buildReference(ref1Id);
            AssetReference ref2 = buildReference(ref2Id);

            when(referenceRepository.findByAssetId(ASSET_ID)).thenReturn(List.of(ref1, ref2));

            List<AssetReferenceResponse> responses = referenceService.getReferencesForAsset(ASSET_ID);

            assertThat(responses).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no references exist")
        void returnsEmptyList() {
            when(referenceRepository.findByAssetId(ASSET_ID)).thenReturn(List.of());

            List<AssetReferenceResponse> responses = referenceService.getReferencesForAsset(ASSET_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("isReferenced")
    class IsReferenced {

        @Test
        @DisplayName("returns true when references exist")
        void returnsTrueWhenReferenced() {
            when(referenceRepository.countByAssetId(ASSET_ID)).thenReturn(2L);
            assertThat(referenceService.isReferenced(ASSET_ID)).isTrue();
        }

        @Test
        @DisplayName("returns false when no references")
        void returnsFalseWhenUnreferenced() {
            when(referenceRepository.countByAssetId(ASSET_ID)).thenReturn(0L);
            assertThat(referenceService.isReferenced(ASSET_ID)).isFalse();
        }
    }
}
