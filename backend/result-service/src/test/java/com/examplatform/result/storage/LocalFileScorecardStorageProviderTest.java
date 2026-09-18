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

package com.examplatform.result.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalFileScorecardStorageProvider Unit Tests")
class LocalFileScorecardStorageProviderTest {

    @TempDir
    Path tempDir;

    private LocalFileScorecardStorageProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalFileScorecardStorageProvider(tempDir);
    }

    @Test
    @DisplayName("Provider name is 'local'")
    void providerName() {
        assertThat(provider.name()).isEqualTo("local");
    }

    @Test
    @DisplayName("Upload stores file and exists returns true")
    void uploadAndExists() throws Exception {
        byte[] content = "%PDF-1.4 test scorecard binary content".getBytes(StandardCharsets.UTF_8);
        String path = "scorecards/scorecard-123.pdf";

        String savedRef = provider.upload(path, new ByteArrayInputStream(content), "application/pdf", content.length);
        assertThat(savedRef).isEqualTo(path);
        assertThat(provider.exists(path)).isTrue();

        Optional<InputStream> downloaded = provider.download(path);
        assertThat(downloaded).isPresent();
        byte[] readBytes = downloaded.get().readAllBytes();
        assertThat(readBytes).isEqualTo(content);
    }

    @Test
    @DisplayName("Delete removes the file")
    void deleteFile() {
        byte[] content = "%PDF-1.4 delete test".getBytes(StandardCharsets.UTF_8);
        String path = "scorecards/scorecard-delete.pdf";

        provider.upload(path, new ByteArrayInputStream(content), "application/pdf", content.length);
        assertThat(provider.exists(path)).isTrue();

        boolean deleted = provider.delete(path);
        assertThat(deleted).isTrue();
        assertThat(provider.exists(path)).isFalse();
    }

    @Test
    @DisplayName("Path traversal attack is prevented")
    void pathTraversalPrevented() {
        byte[] content = "malicious".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> provider.upload("../outside.pdf", new ByteArrayInputStream(content), "application/pdf", content.length))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal attempt detected");
    }

    @Test
    @DisplayName("Generate presigned URL returns local download URL")
    void generatePresignedUrl() {
        String url = provider.generatePresignedUrl("scorecards/scorecard-1.pdf", Duration.ofMinutes(15));
        assertThat(url).contains("/api/v1/results/scorecard/download?ref=scorecards/scorecard-1.pdf");
    }

    @Test
    @DisplayName("Health returns true when directory is accessible")
    void healthCheck() {
        assertThat(provider.health()).isTrue();
    }
}
