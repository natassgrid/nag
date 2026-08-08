package com.examplatform.asset.storage;

import java.io.InputStream;
import java.util.Optional;

/**
 * Service Provider Interface for pluggable asset storage backends.
 *
 * <p>Implementations must be thread-safe. The platform ships with a
 * {@code LocalFileSystemStorageProvider}; additional providers (S3, Azure Blob,
 * GCS, MinIO, NFS) can be added without modifying this interface.
 *
 * <p>Each provider is identified by a unique {@link #name()} used in
 * configuration and persisted on the asset record.
 */
public interface StorageProvider {

    /**
     * Unique identifier for this storage provider (e.g. "filesystem", "s3", "azure-blob").
     * This value is stored on the asset record to identify where the binary resides.
     */
    String name();

    /**
     * Upload binary content to the storage backend.
     *
     * @param path        the logical path/key where the asset should be stored
     * @param content     the binary content stream
     * @param contentType the MIME type of the content
     * @param size        the size in bytes of the content
     * @return the resolved storage location (provider-specific URI or path)
     */
    String upload(String path, InputStream content, String contentType, long size);

    /**
     * Download binary content from the storage backend.
     *
     * @param storageLocation the storage location returned by {@link #upload}
     * @return an InputStream of the binary content, or empty if not found
     */
    Optional<InputStream> download(String storageLocation);

    /**
     * Delete binary content from the storage backend.
     *
     * @param storageLocation the storage location returned by {@link #upload}
     * @return true if the file was deleted, false if it didn't exist
     */
    boolean delete(String storageLocation);

    /**
     * Check if a file exists at the given storage location.
     *
     * @param storageLocation the storage location to check
     * @return true if the file exists
     */
    boolean exists(String storageLocation);

    /**
     * Resolve a storage location to an accessible URL or path.
     * For local filesystem this returns the absolute path;
     * for cloud providers this may return a pre-signed URL.
     *
     * @param storageLocation the storage location to resolve
     * @return the resolved URL or path
     */
    String resolve(String storageLocation);

    /**
     * Check the health of this storage provider.
     *
     * @return true if the provider is operational
     */
    boolean health();
}
