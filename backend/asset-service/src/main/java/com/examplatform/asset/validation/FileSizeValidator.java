package com.examplatform.asset.validation;

import com.examplatform.asset.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates that the uploaded file does not exceed the configured maximum size.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSizeValidator {

    private final StorageProperties storageProperties;

    /**
     * Validate file size against configured maximum.
     *
     * @param fileSize the size in bytes of the uploaded file
     * @throws AssetValidationException if the file exceeds the maximum allowed size
     */
    public void validate(long fileSize) {
        long maxSize = storageProperties.getMaxFileSize();

        if (fileSize <= 0) {
            throw new AssetValidationException("File is empty (size=0 bytes)");
        }

        if (fileSize > maxSize) {
            throw new AssetValidationException(
                    "File size " + formatSize(fileSize) + " exceeds maximum allowed "
                            + formatSize(maxSize));
        }

        log.debug("File size validation passed: {} <= {}", formatSize(fileSize), formatSize(maxSize));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
