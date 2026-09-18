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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;

/**
 * Storage provider implementation that uses the local file system for scorecards.
 *
 * <p>Scorecards are stored under a configurable base directory.
 * This is the fallback provider for development, testing, and single-node/docker environments.
 */
@Slf4j
@Component
public class LocalFileScorecardStorageProvider implements ScorecardStorageProvider {

    private final Path basePath;

    @Autowired
    public LocalFileScorecardStorageProvider(ScorecardStorageProperties properties) {
        this.basePath = Paths.get(properties.getEffectiveLocalPath()).toAbsolutePath().normalize();
        ensureBaseDirectory();
        log.info("LocalFileScorecardStorageProvider initialized with base path: {}", this.basePath);
    }

    public LocalFileScorecardStorageProvider(Path basePath) {
        this.basePath = basePath.toAbsolutePath().normalize();
        ensureBaseDirectory();
        log.info("LocalFileScorecardStorageProvider initialized with base path: {}", this.basePath);
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public String upload(String path, InputStream content, String contentType, long size) {
        Path target = resolveSecurePath(path);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Saved scorecard to local file: {}", target);
            return path;
        } catch (IOException e) {
            log.error("Failed to write scorecard to local file: {}", target, e);
            throw new RuntimeException("Failed to save scorecard to local storage: " + path, e);
        }
    }

    @Override
    public Optional<InputStream> download(String path) {
        Path target = resolveSecurePath(path);
        if (!Files.exists(target)) {
            // Also try resolving directly in case path already has directory prefix
            Path directTarget = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();
            if (Files.exists(directTarget)) {
                target = directTarget;
            } else {
                return Optional.empty();
            }
        }
        try {
            return Optional.of(Files.newInputStream(target));
        } catch (IOException e) {
            log.error("Failed to read scorecard from local file: {}", target, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String path) {
        Path target = resolveSecurePath(path);
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            log.error("Failed to delete local scorecard: {}", target, e);
            return false;
        }
    }

    @Override
    public boolean exists(String path) {
        Path target = resolveSecurePath(path);
        if (Files.exists(target)) {
            return true;
        }
        Path directTarget = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();
        return Files.exists(directTarget);
    }

    @Override
    public String generatePresignedUrl(String path, Duration expiration) {
        // Local files do not have presigned URLs; return a direct download API path
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        return "/api/v1/results/scorecard/download?ref=" + cleanPath;
    }

    @Override
    public boolean health() {
        try {
            return Files.exists(basePath) && Files.isWritable(basePath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve path safely preventing path traversal attacks outside basePath.
     */
    private Path resolveSecurePath(String relativePath) {
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        Path resolved = basePath.resolve(cleanPath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("Path traversal attempt detected: " + relativePath);
        }
        return resolved;
    }

    private void ensureBaseDirectory() {
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.warn("Could not create local scorecard storage base directory: {}", basePath, e);
        }
    }
}
