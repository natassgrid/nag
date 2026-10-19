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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing storage, SHA-256 integrity verification, and memory caching
 * for uploaded Question Paper PDF documents.
 */
@Slf4j
@Service
public class PaperFileStorageService {

    private static final int MAX_CACHE_ENTRIES = 10;

    @Value("${app.extraction.storage-path:./paper-extraction-storage}")
    private String storageBasePath = "./paper-extraction-storage";

    /**
     * In-memory cache holding recently uploaded PDF byte buffers so immediate
     * async extraction jobs can execute without disk read latency.
     */
    private final Map<UUID, byte[]> recentPdfCache = new ConcurrentHashMap<>();

    /**
     * Computes the hexadecimal SHA-256 hash string for the provided byte buffer.
     *
     * @param bytes binary data
     * @return 64-character lowercase hexadecimal hash
     */
    public String computeSha256(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest algorithm unavailable", e);
        }
    }

    /**
     * Persists uploaded PDF bytes to disk with path-traversal safeguards and keeps
     * an in-memory buffer copy for immediate extraction.
     *
     * @param paperId unique ID of the question paper
     * @param tenantId examination authority identifier
     * @param originalFilename original name of the uploaded file
     * @param pdfBytes binary content of the PDF
     * @return absolute path of the saved file on disk
     */
    public String storePdf(UUID paperId, String tenantId, String originalFilename, byte[] pdfBytes) {
        if (paperId == null) {
            throw new IllegalArgumentException("paperId must not be null");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes must not be null or empty");
        }

        try {
            Path baseDir = Paths.get(storageBasePath).toAbsolutePath().normalize();
            String safeTenantId = tenantId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            Path tenantDir = baseDir.resolve(safeTenantId).normalize();

            if (!tenantDir.startsWith(baseDir)) {
                throw new SecurityException("Path traversal attempt in tenant directory resolution: " + tenantId);
            }

            Files.createDirectories(tenantDir);

            String filename = paperId + ".pdf";
            Path targetFile = tenantDir.resolve(filename).normalize();

            if (!targetFile.startsWith(tenantDir)) {
                throw new SecurityException("Path traversal attempt in paper file resolution: " + paperId);
            }

            Files.write(targetFile, pdfBytes);
            log.info("Stored question paper PDF for paperId={} at path={}", paperId, targetFile);

            // Maintain bounded in-memory cache
            if (recentPdfCache.size() >= MAX_CACHE_ENTRIES) {
                Iterator<UUID> iterator = recentPdfCache.keySet().iterator();
                if (iterator.hasNext()) {
                    UUID evicted = iterator.next();
                    iterator.remove();
                    log.debug("Evicted oldest entry {} from recentPdfCache", evicted);
                }
            }
            recentPdfCache.put(paperId, pdfBytes);

            return targetFile.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write question paper PDF to disk: " + paperId, e);
        }
    }

    /**
     * Loads the PDF binary bytes, consulting the in-memory cache first before
     * reading from disk.
     *
     * @param paperId unique ID of the question paper
     * @param fileStoragePath disk storage path of the PDF
     * @return PDF binary byte array
     */
    public byte[] loadPdfBytes(UUID paperId, String fileStoragePath) {
        if (paperId != null) {
            byte[] cached = recentPdfCache.get(paperId);
            if (cached != null) {
                log.debug("Loaded PDF for paperId={} directly from in-memory cache", paperId);
                return cached;
            }
        }

        if (fileStoragePath == null || fileStoragePath.isBlank()) {
            throw new IllegalArgumentException("fileStoragePath must not be null or blank");
        }

        Path path = Paths.get(fileStoragePath).toAbsolutePath().normalize();
        Path baseDir = Paths.get(storageBasePath).toAbsolutePath().normalize();

        if (!path.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt in fileStoragePath: " + fileStoragePath);
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read PDF bytes from path: " + fileStoragePath, e);
        }
    }

    /**
     * Removes the PDF buffer from the in-memory cache once asynchronous processing completes.
     *
     * @param paperId unique ID of the question paper
     */
    public void evictMemoryCache(UUID paperId) {
        if (paperId != null) {
            recentPdfCache.remove(paperId);
            log.debug("Evicted paperId={} from recentPdfCache", paperId);
        }
    }
}
