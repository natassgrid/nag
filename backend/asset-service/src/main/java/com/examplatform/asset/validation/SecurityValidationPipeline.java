package com.examplatform.asset.validation;

import com.examplatform.asset.domain.enums.AssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Orchestrates the full validation pipeline for asset uploads.
 *
 * <p>Pipeline steps:
 * <ol>
 *   <li>Filename sanitization</li>
 *   <li>File size validation</li>
 *   <li>MIME type validation (against allowed list)</li>
 *   <li>Magic number validation (content vs declared type)</li>
 * </ol>
 *
 * <p>All validations run before the file is persisted to storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityValidationPipeline {

    private final FilenameSanitizer filenameSanitizer;
    private final FileSizeValidator fileSizeValidator;
    private final MimeValidator mimeValidator;
    private final MagicNumberValidator magicNumberValidator;

    /**
     * Run the full validation pipeline.
     *
     * @param filename    the original filename
     * @param contentType the declared content type
     * @param size        the file size in bytes
     * @param content     the file content stream (must support mark/reset)
     * @return validation result with sanitized filename and classified asset type
     */
    public ValidationResult validate(String filename, String contentType, long size, InputStream content) {
        log.info("Running security validation pipeline for file: {}", filename);

        // 1. Sanitize filename
        String sanitizedFilename = filenameSanitizer.sanitize(filename);
        String extension = filenameSanitizer.extractExtension(sanitizedFilename);

        // 2. Validate file size
        fileSizeValidator.validate(size);

        // 3. Validate MIME type against allowed list
        AssetType assetType = mimeValidator.validate(contentType);

        // 4. Validate magic numbers match declared type
        String detectedMimeType = magicNumberValidator.validateAndDetect(content, contentType, sanitizedFilename);

        log.info("Validation passed: file='{}', type={}, detectedMime={}, size={}",
                sanitizedFilename, assetType, detectedMimeType, size);

        return ValidationResult.builder()
                .sanitizedFilename(sanitizedFilename)
                .extension(extension)
                .assetType(assetType)
                .detectedContentType(detectedMimeType)
                .build();
    }

    /**
     * Compute SHA-256 hash of the given byte array.
     *
     * @param data the file content bytes
     * @return hex-encoded SHA-256 hash
     */
    public static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Result of the security validation pipeline.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ValidationResult {
        private String sanitizedFilename;
        private String extension;
        private AssetType assetType;
        private String detectedContentType;
    }
}
