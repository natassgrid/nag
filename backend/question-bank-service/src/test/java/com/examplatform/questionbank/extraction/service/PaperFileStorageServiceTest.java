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

package com.examplatform.questionbank.extraction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaperFileStorageService Unit Tests")
class PaperFileStorageServiceTest {

    private PaperFileStorageService storageService;

    @TempDir
    Path tempStorageDir;

    @BeforeEach
    void setUp() {
        storageService = new PaperFileStorageService();
        ReflectionTestUtils.setField(storageService, "storageBasePath", tempStorageDir.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("computeSha256 calculates expected hex hash")
    void testComputeSha256() {
        byte[] data = "Sample PDF byte content".getBytes(StandardCharsets.UTF_8);
        String hash = storageService.computeSha256(data);
        assertThat(hash).isNotNull().hasSize(64);

        // Empty bytes
        assertThat(storageService.computeSha256(new byte[0])).isEmpty();
        assertThat(storageService.computeSha256(null)).isEmpty();
    }

    @Test
    @DisplayName("storePdf and loadPdfBytes work with caching and disk persistence")
    void testStoreAndLoadPdfBytes() {
        UUID paperId = UUID.randomUUID();
        String tenantId = "test-tenant-1";
        byte[] pdfBytes = "%PDF-1.4 sample content".getBytes(StandardCharsets.UTF_8);

        String path = storageService.storePdf(paperId, tenantId, "physics.pdf", pdfBytes);
        assertThat(path).isNotNull();
        assertThat(new File(path)).exists();

        // Load via cache
        byte[] cachedBytes = storageService.loadPdfBytes(paperId, path);
        assertThat(cachedBytes).isEqualTo(pdfBytes);

        // Evict cache and load directly from disk
        storageService.evictMemoryCache(paperId);
        byte[] diskBytes = storageService.loadPdfBytes(paperId, path);
        assertThat(diskBytes).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("storePdf rejects null inputs")
    void testStorePdfInputValidation() {
        UUID paperId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};

        assertThatThrownBy(() -> storageService.storePdf(null, "tenant", "file.pdf", bytes))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> storageService.storePdf(paperId, "", "file.pdf", bytes))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> storageService.storePdf(paperId, "tenant", "file.pdf", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
