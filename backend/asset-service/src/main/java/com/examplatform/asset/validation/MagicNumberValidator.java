package com.examplatform.asset.validation;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Validates file content by inspecting magic bytes using Apache Tika.
 * Ensures the actual file content matches the declared MIME type,
 * preventing MIME spoofing attacks.
 */
@Slf4j
@Component
public class MagicNumberValidator {

    private final Tika tika = new Tika();

    /**
     * Detect the actual MIME type from file content and validate against the declared type.
     *
     * @param content         the file content stream (must support mark/reset)
     * @param declaredMimeType the MIME type declared by the client
     * @param filename         the original filename for Tika hint
     * @return the detected MIME type
     * @throws AssetValidationException if the detected type doesn't match the declared type
     */
    public String validateAndDetect(InputStream content, String declaredMimeType, String filename) {
        try {
            BufferedInputStream buffered = content instanceof BufferedInputStream
                    ? (BufferedInputStream) content
                    : new BufferedInputStream(content);
            buffered.mark(65536);

            String detectedType = tika.detect(buffered, filename);
            buffered.reset();

            String normalizedDeclared = declaredMimeType.toLowerCase().split(";")[0].trim();
            String normalizedDetected = detectedType.toLowerCase();

            // Allow close matches (e.g. audio/mpeg vs audio/mp3, image/svg+xml vs application/xml)
            if (!isCompatible(normalizedDeclared, normalizedDetected)) {
                throw new AssetValidationException(
                        "MIME type mismatch: declared=" + normalizedDeclared
                                + ", detected=" + normalizedDetected
                                + ". Possible file content spoofing.");
            }

            log.debug("Magic number validation passed: declared={}, detected={}", normalizedDeclared, normalizedDetected);
            return normalizedDetected;

        } catch (IOException e) {
            throw new AssetValidationException("Failed to read file for magic number validation", e);
        }
    }

    /**
     * Check if declared and detected MIME types are compatible.
     * Some formats have multiple valid MIME representations.
     */
    private boolean isCompatible(String declared, String detected) {
        if (declared.equals(detected)) return true;

        // Same primary type is generally acceptable
        String declaredPrimary = declared.split("/")[0];
        String detectedPrimary = detected.split("/")[0];
        if (declaredPrimary.equals(detectedPrimary)) return true;

        // SVG may be detected as application/xml
        if (declared.equals("image/svg+xml") && detected.contains("xml")) return true;

        // WAV variants
        if (declared.startsWith("audio/") && detected.startsWith("audio/")) return true;

        return false;
    }
}
