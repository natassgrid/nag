package com.examplatform.asset.service;

import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import com.examplatform.asset.dto.AssetMetadataUpdateRequest;
import com.examplatform.asset.dto.AssetUploadResponse;
import com.examplatform.asset.metadata.MediaMetadata;
import com.examplatform.asset.metadata.MetadataExtractionService;
import com.examplatform.asset.repository.AssetReferenceRepository;
import com.examplatform.asset.repository.MediaAssetRepository;
import com.examplatform.asset.storage.StorageProperties;
import com.examplatform.asset.storage.StorageProvider;
import com.examplatform.asset.storage.StorageProviderRegistry;
import com.examplatform.asset.validation.AssetValidationException;
import com.examplatform.asset.validation.SecurityValidationPipeline;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AssetService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetService")
class AssetServiceTest {

    @Mock
    private MediaAssetRepository assetRepository;

    @Mock
    private AssetReferenceRepository referenceRepository;

    @Mock
    private SecurityValidationPipeline validationPipeline;

    @Mock
    private MetadataExtractionService metadataExtractionService;

    @Mock
    private StorageProviderRegistry storageProviderRegistry;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AssetService assetService;

    @Captor
    private ArgumentCaptor<MediaAsset> assetCaptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-test";

    private MediaAsset buildAsset(UUID id, AssetStatus status) {
        MediaAsset asset = MediaAsset.builder()
                .originalFilename("test.png")
                .contentType("image/png")
                .extension("png")
                .fileSize(1024L)
                .sha256Hash("abc123def456")
                .assetType(AssetType.IMAGE)
                .status(status)
                .storageProvider("filesystem")
                .storageLocation("tenant/2024/01/01/abc1/abc123_test.png")
                .createdBy(USER_ID)
                .build();
        // Set BaseEntity fields via reflection
        try {
            var setIdMethod = asset.getClass().getSuperclass().getDeclaredMethod("setId", UUID.class);
            setIdMethod.setAccessible(true);
            setIdMethod.invoke(asset, id);
            var createdAtField = asset.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(asset, Instant.now());
            var updatedAtField = asset.getClass().getSuperclass().getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(asset, Instant.now());
            var tenantField = asset.getClass().getSuperclass().getDeclaredField("tenantId");
            tenantField.setAccessible(true);
            tenantField.set(asset, TENANT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return asset;
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("validates, extracts metadata, stores, and persists asset")
        void fullUploadFlow() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            byte[] content = "fake-image-content".getBytes();
            when(file.getBytes()).thenReturn(content);
            when(file.getOriginalFilename()).thenReturn("photo.png");
            when(file.getContentType()).thenReturn("image/png");
            when(file.getSize()).thenReturn((long) content.length);

            SecurityValidationPipeline.ValidationResult validationResult =
                    SecurityValidationPipeline.ValidationResult.builder()
                            .sanitizedFilename("photo.png")
                            .extension("png")
                            .assetType(AssetType.IMAGE)
                            .detectedContentType("image/png")
                            .build();
            when(validationPipeline.validate(anyString(), anyString(), anyLong(), any(InputStream.class)))
                    .thenReturn(validationResult);

            when(assetRepository.findBySha256HashAndTenantId(anyString(), eq(TENANT_ID)))
                    .thenReturn(Optional.empty());

            MediaMetadata metadata = MediaMetadata.builder().width(800).height(600).dpi(72).build();
            when(metadataExtractionService.extract(eq(AssetType.IMAGE), any(InputStream.class), eq("image/png")))
                    .thenReturn(metadata);

            when(storageProperties.getProvider()).thenReturn("filesystem");
            StorageProvider storageProvider = mock(StorageProvider.class);
            when(storageProviderRegistry.requireProvider("filesystem")).thenReturn(storageProvider);
            when(storageProvider.name()).thenReturn("filesystem");
            when(storageProvider.upload(anyString(), any(InputStream.class), anyString(), anyLong()))
                    .thenReturn("tenant-test/2024/01/01/abc1/hash_photo.png");

            when(assetRepository.save(any(MediaAsset.class))).thenAnswer(invocation -> {
                MediaAsset saved = invocation.getArgument(0);
                try {
                    var setIdMethod = saved.getClass().getSuperclass().getDeclaredMethod("setId", UUID.class);
                    setIdMethod.setAccessible(true);
                    setIdMethod.invoke(saved, UUID.randomUUID());
                    var createdAtField = saved.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(saved, Instant.now());
                    var updatedAtField = saved.getClass().getSuperclass().getDeclaredField("updatedAt");
                    updatedAtField.setAccessible(true);
                    updatedAtField.set(saved, Instant.now());
                } catch (Exception e) {
                    // ignore
                }
                return saved;
            });

            // When
            AssetUploadResponse response = assetService.upload(file, USER_ID, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOriginalFilename()).isEqualTo("photo.png");
            assertThat(response.getAssetType()).isEqualTo(AssetType.IMAGE);
            assertThat(response.getStatus()).isEqualTo(AssetStatus.ACTIVE);
            assertThat(response.getWidth()).isEqualTo(800);
            assertThat(response.getHeight()).isEqualTo(600);
            assertThat(response.getStorageProvider()).isEqualTo("filesystem");

            verify(validationPipeline).validate(anyString(), anyString(), anyLong(), any(InputStream.class));
            verify(metadataExtractionService).extract(eq(AssetType.IMAGE), any(InputStream.class), eq("image/png"));
            verify(storageProvider).upload(anyString(), any(InputStream.class), eq("image/png"), anyLong());
            verify(assetRepository).save(any(MediaAsset.class));
        }

        @Test
        @DisplayName("returns existing asset on duplicate hash")
        void returnsDuplicateAsset() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            byte[] content = "duplicate".getBytes();
            when(file.getBytes()).thenReturn(content);
            when(file.getOriginalFilename()).thenReturn("dup.png");
            when(file.getContentType()).thenReturn("image/png");
            when(file.getSize()).thenReturn((long) content.length);

            SecurityValidationPipeline.ValidationResult validationResult =
                    SecurityValidationPipeline.ValidationResult.builder()
                            .sanitizedFilename("dup.png")
                            .extension("png")
                            .assetType(AssetType.IMAGE)
                            .detectedContentType("image/png")
                            .build();
            when(validationPipeline.validate(anyString(), anyString(), anyLong(), any(InputStream.class)))
                    .thenReturn(validationResult);

            UUID existingId = UUID.randomUUID();
            MediaAsset existing = buildAsset(existingId, AssetStatus.ACTIVE);
            when(assetRepository.findBySha256HashAndTenantId(anyString(), eq(TENANT_ID)))
                    .thenReturn(Optional.of(existing));

            // When
            AssetUploadResponse response = assetService.upload(file, USER_ID, TENANT_ID);

            // Then - returns existing, doesn't store again
            assertThat(response.getId()).isEqualTo(existingId);
            verify(assetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAsset")
    class GetAsset {

        @Test
        @DisplayName("returns asset response for existing asset")
        void returnsExistingAsset() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            AssetUploadResponse response = assetService.getAsset(assetId);

            assertThat(response.getId()).isEqualTo(assetId);
            assertThat(response.getOriginalFilename()).isEqualTo("test.png");
        }

        @Test
        @DisplayName("throws EntityNotFoundException for missing asset")
        void throwsForMissingAsset() {
            UUID assetId = UUID.randomUUID();
            when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.getAsset(assetId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(assetId.toString());
        }
    }

    @Nested
    @DisplayName("updateMetadata")
    class UpdateMetadata {

        @Test
        @DisplayName("updates title, description, altText, tags, and language")
        void updatesAllMetadataFields() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(MediaAsset.class))).thenAnswer(i -> i.getArgument(0));

            AssetMetadataUpdateRequest request = AssetMetadataUpdateRequest.builder()
                    .title("New Title")
                    .description("New Description")
                    .altText("Alt text for accessibility")
                    .tags("math,calculus")
                    .language("en")
                    .build();

            AssetUploadResponse response = assetService.updateMetadata(assetId, request, USER_ID, TENANT_ID);

            assertThat(response.getTitle()).isEqualTo("New Title");
            assertThat(response.getDescription()).isEqualTo("New Description");
            assertThat(response.getAltText()).isEqualTo("Alt text for accessibility");
            assertThat(response.getTags()).isEqualTo("math,calculus");
            assertThat(response.getLanguage()).isEqualTo("en");
        }

        @Test
        @DisplayName("only updates non-null fields")
        void updatesOnlyNonNullFields() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            asset.setTitle("Original Title");
            asset.setDescription("Original Description");
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(MediaAsset.class))).thenAnswer(i -> i.getArgument(0));

            AssetMetadataUpdateRequest request = AssetMetadataUpdateRequest.builder()
                    .title("Updated Title")
                    .build();

            AssetUploadResponse response = assetService.updateMetadata(assetId, request, USER_ID, TENANT_ID);

            assertThat(response.getTitle()).isEqualTo("Updated Title");
            assertThat(response.getDescription()).isEqualTo("Original Description");
        }
    }

    @Nested
    @DisplayName("deleteAsset")
    class DeleteAsset {

        @Test
        @DisplayName("soft-deletes unreferenced asset")
        void softDeletesUnreferencedAsset() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(referenceRepository.countByAssetId(assetId)).thenReturn(0L);
            when(assetRepository.save(any(MediaAsset.class))).thenAnswer(i -> i.getArgument(0));

            assetService.deleteAsset(assetId, USER_ID, TENANT_ID);

            verify(assetRepository).save(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AssetStatus.DELETED);
        }

        @Test
        @DisplayName("rejects deletion of referenced asset")
        void rejectsDeletionOfReferencedAsset() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(referenceRepository.countByAssetId(assetId)).thenReturn(3L);

            assertThatThrownBy(() -> assetService.deleteAsset(assetId, USER_ID, TENANT_ID))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("still referenced")
                    .hasMessageContaining("3");

            verify(assetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("archiveAsset")
    class ArchiveAsset {

        @Test
        @DisplayName("archives ACTIVE asset")
        void archivesActiveAsset() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(MediaAsset.class))).thenAnswer(i -> i.getArgument(0));

            AssetUploadResponse response = assetService.archiveAsset(assetId, USER_ID, TENANT_ID);

            assertThat(response.getStatus()).isEqualTo(AssetStatus.ARCHIVED);
        }

        @Test
        @DisplayName("rejects archiving non-ACTIVE asset")
        void rejectsArchivingNonActive() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ARCHIVED);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> assetService.archiveAsset(assetId, USER_ID, TENANT_ID))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Only ACTIVE");
        }
    }

    @Nested
    @DisplayName("restoreAsset")
    class RestoreAsset {

        @Test
        @DisplayName("restores ARCHIVED asset to ACTIVE")
        void restoresArchivedAsset() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ARCHIVED);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(MediaAsset.class))).thenAnswer(i -> i.getArgument(0));

            AssetUploadResponse response = assetService.restoreAsset(assetId, USER_ID, TENANT_ID);

            assertThat(response.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        }

        @Test
        @DisplayName("rejects restoring non-ARCHIVED asset")
        void rejectsRestoringNonArchived() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> assetService.restoreAsset(assetId, USER_ID, TENANT_ID))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Only ARCHIVED");
        }
    }

    @Nested
    @DisplayName("downloadAsset")
    class DownloadAsset {

        @Test
        @DisplayName("delegates to storage provider and returns stream")
        void delegatesToStorageProvider() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            StorageProvider provider = mock(StorageProvider.class);
            InputStream expectedStream = new ByteArrayInputStream("content".getBytes());
            when(storageProviderRegistry.requireProvider("filesystem")).thenReturn(provider);
            when(provider.download("tenant/2024/01/01/abc1/abc123_test.png"))
                    .thenReturn(Optional.of(expectedStream));

            Optional<InputStream> result = assetService.downloadAsset(assetId);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(expectedStream);
        }

        @Test
        @DisplayName("returns empty when storage file is missing")
        void returnsEmptyWhenFileMissing() {
            UUID assetId = UUID.randomUUID();
            MediaAsset asset = buildAsset(assetId, AssetStatus.ACTIVE);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            StorageProvider provider = mock(StorageProvider.class);
            when(storageProviderRegistry.requireProvider("filesystem")).thenReturn(provider);
            when(provider.download(anyString())).thenReturn(Optional.empty());

            Optional<InputStream> result = assetService.downloadAsset(assetId);

            assertThat(result).isEmpty();
        }
    }
}
