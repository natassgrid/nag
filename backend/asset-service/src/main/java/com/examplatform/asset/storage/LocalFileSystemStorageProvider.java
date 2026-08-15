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

package com.examplatform.asset.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Storage provider implementation that uses the local file system.
 *
 * <p>Files are stored under a configurable base directory, organized by
 * tenant and date-based subdirectories to prevent excessive files in a single folder.
 *
 * <p>This is the default provider for development and single-node deployments.
 */
@Slf4j
@Component
public class LocalFileSystemStorageProvider implements StorageProvider {

    private final Path basePath;

    public LocalFileSystemStorageProvider(StorageProperties properties) {
        this.basePath = Paths.get(properties.getFilesystem().getBasePath()).toAbsolutePath().normalize();
        ensureBaseDirectory();
        log.info("LocalFileSystemStorageProvider initialized with base path: {}", this.basePath);
    }

    @Override
    public String name() {
        return "filesystem";
    }

    @Override
    public String upload(String path, InputStream content, String contentType, long size) {
        Path targetPath = resolve(basePath, path);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored asset at: {}", targetPath);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store asset at: " + targetPath, e);
        }
    }

    @Override
    public Optional<InputStream> download(String storageLocation) {
        Path filePath = resolve(basePath, storageLocation);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.newInputStream(filePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read asset: " + filePath, e);
        }
    }

    @Override
    public boolean delete(String storageLocation) {
        Path filePath = resolve(basePath, storageLocation);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete asset: " + filePath, e);
        }
    }

    @Override
    public boolean exists(String storageLocation) {
        Path filePath = resolve(basePath, storageLocation);
        return Files.exists(filePath);
    }

    @Override
    public String resolve(String storageLocation) {
        return resolve(basePath, storageLocation).toAbsolutePath().toString();
    }

    @Override
    public boolean health() {
        return Files.isDirectory(basePath) && Files.isWritable(basePath);
    }

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Resolves a relative storage path against the base, preventing path traversal.
     */
    private Path resolve(Path base, String relativePath) {
        Path resolved = base.resolve(relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }
        return resolved;
    }

    private void ensureBaseDirectory() {
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create storage base directory: " + basePath, e);
        }
    }
}
