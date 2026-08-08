package com.examplatform.asset.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for asset storage.
 *
 * <pre>
 * asset:
 *   storage:
 *     provider: filesystem
 *     filesystem:
 *       base-path: ./asset-storage
 *     max-file-size: 104857600  # 100 MB
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "asset.storage")
public class StorageProperties {

    /** The active storage provider name (e.g. "filesystem", "s3"). */
    private String provider = "filesystem";

    /** Maximum allowed file size in bytes (default 100 MB). */
    private long maxFileSize = 104_857_600L;

    /** Filesystem-specific configuration. */
    private FilesystemProperties filesystem = new FilesystemProperties();

    @Data
    public static class FilesystemProperties {
        /** Base directory for storing files. */
        private String basePath = "./asset-storage";
    }
}
