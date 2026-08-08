package com.examplatform.asset.validation;

import com.examplatform.asset.domain.enums.AssetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Validates that the uploaded file's MIME type is in the allowed set
 * and maps it to the corresponding {@link AssetType}.
 */
@Slf4j
@Component
public class MimeValidator {

    private static final Map<String, AssetType> ALLOWED_MIME_TYPES = Map.ofEntries(
            // Images
            Map.entry("image/png", AssetType.IMAGE),
            Map.entry("image/jpeg", AssetType.IMAGE),
            Map.entry("image/webp", AssetType.IMAGE),
            Map.entry("image/svg+xml", AssetType.IMAGE),
            // Audio
            Map.entry("audio/mpeg", AssetType.AUDIO),
            Map.entry("audio/mp3", AssetType.AUDIO),
            Map.entry("audio/aac", AssetType.AUDIO),
            Map.entry("audio/wav", AssetType.AUDIO),
            Map.entry("audio/x-wav", AssetType.AUDIO),
            Map.entry("audio/wave", AssetType.AUDIO),
            // Video
            Map.entry("video/mp4", AssetType.VIDEO)
    );

    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
            "application/x-executable",
            "application/x-msdos-program",
            "application/x-msdownload",
            "application/x-sh",
            "application/x-shellscript",
            "application/java-archive",
            "application/x-java-class",
            "application/javascript"
    );

    /**
     * Validate the MIME type and return the corresponding asset type.
     *
     * @param contentType the detected MIME type
     * @return the asset type classification
     * @throws AssetValidationException if the MIME type is blocked or not supported
     */
    public AssetType validate(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new AssetValidationException("Content type is required");
        }

        String normalized = contentType.toLowerCase().split(";")[0].trim();

        if (BLOCKED_MIME_TYPES.contains(normalized)) {
            throw new AssetValidationException("Blocked MIME type: " + normalized);
        }

        AssetType type = ALLOWED_MIME_TYPES.get(normalized);
        if (type == null) {
            throw new AssetValidationException(
                    "Unsupported MIME type: " + normalized + ". Allowed: " + ALLOWED_MIME_TYPES.keySet());
        }

        log.debug("MIME validation passed: {} → {}", normalized, type);
        return type;
    }

    /**
     * Check if a content type is in the allowed set.
     */
    public boolean isSupported(String contentType) {
        if (contentType == null) return false;
        String normalized = contentType.toLowerCase().split(";")[0].trim();
        return ALLOWED_MIME_TYPES.containsKey(normalized);
    }
}
