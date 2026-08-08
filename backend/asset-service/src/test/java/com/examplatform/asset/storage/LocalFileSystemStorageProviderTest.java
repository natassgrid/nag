package com.examplatform.asset.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LocalFileSystemStorageProvider}.
 */
@DisplayName("LocalFileSystemStorageProvider")
class LocalFileSystemStorageProviderTest {

    @TempDir
    Path tempDir;

    private LocalFileSystemStorageProvider provider;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setFilesystem(new StorageProperties.FilesystemProperties());
        properties.getFilesystem().setBasePath(tempDir.toString());
        provider = new LocalFileSystemStorageProvider(properties);
    }

    @Test
    @DisplayName("name() returns 'filesystem'")
    void nameReturnsFilesystem() {
        assertThat(provider.name()).isEqualTo("filesystem");
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("stores file and returns the path")
        void storesFileAndReturnsPath() throws IOException {
            byte[] content = "Hello, asset!".getBytes();
            InputStream input = new ByteArrayInputStream(content);

            String location = provider.upload("tenant/2024/01/01/file.png", input, "image/png", content.length);

            assertThat(location).isEqualTo("tenant/2024/01/01/file.png");
            Path stored = tempDir.resolve("tenant/2024/01/01/file.png");
            assertThat(stored).exists();
            assertThat(Files.readAllBytes(stored)).isEqualTo(content);
        }

        @Test
        @DisplayName("creates nested directories automatically")
        void createsNestedDirectories() {
            byte[] content = "data".getBytes();
            InputStream input = new ByteArrayInputStream(content);

            provider.upload("a/b/c/d/file.txt", input, "text/plain", content.length);

            assertThat(tempDir.resolve("a/b/c/d/file.txt")).exists();
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("returns content for existing file")
        void returnsContentForExistingFile() throws IOException {
            byte[] content = "download me".getBytes();
            provider.upload("test/download.txt", new ByteArrayInputStream(content), "text/plain", content.length);

            Optional<InputStream> result = provider.download("test/download.txt");

            assertThat(result).isPresent();
            assertThat(result.get().readAllBytes()).isEqualTo(content);
        }

        @Test
        @DisplayName("returns empty for non-existent file")
        void returnsEmptyForMissingFile() {
            Optional<InputStream> result = provider.download("does/not/exist.txt");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true for stored file")
        void returnsTrueForStoredFile() {
            byte[] content = "exists".getBytes();
            provider.upload("existing.txt", new ByteArrayInputStream(content), "text/plain", content.length);

            assertThat(provider.exists("existing.txt")).isTrue();
        }

        @Test
        @DisplayName("returns false for missing file")
        void returnsFalseForMissingFile() {
            assertThat(provider.exists("missing.txt")).isFalse();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes existing file and returns true")
        void deletesExistingFile() {
            byte[] content = "delete me".getBytes();
            provider.upload("to-delete.txt", new ByteArrayInputStream(content), "text/plain", content.length);

            boolean deleted = provider.delete("to-delete.txt");

            assertThat(deleted).isTrue();
            assertThat(provider.exists("to-delete.txt")).isFalse();
        }

        @Test
        @DisplayName("returns false for non-existent file")
        void returnsFalseForMissingFile() {
            assertThat(provider.delete("no-such-file.txt")).isFalse();
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns absolute path within base directory")
        void returnsAbsolutePath() {
            String resolved = provider.resolve("tenant/file.png");
            assertThat(resolved).startsWith(tempDir.toAbsolutePath().toString());
            assertThat(resolved).endsWith("file.png");
        }
    }

    @Nested
    @DisplayName("health")
    class Health {

        @Test
        @DisplayName("returns true when base directory exists and is writable")
        void returnsTrueWhenHealthy() {
            assertThat(provider.health()).isTrue();
        }
    }

    @Nested
    @DisplayName("security")
    class Security {

        @Test
        @DisplayName("rejects path traversal attempts")
        void rejectsPathTraversal() {
            byte[] content = "evil".getBytes();
            InputStream input = new ByteArrayInputStream(content);

            assertThatThrownBy(() -> provider.upload("../../../etc/passwd", input, "text/plain", content.length))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("traversal");
        }
    }
}
