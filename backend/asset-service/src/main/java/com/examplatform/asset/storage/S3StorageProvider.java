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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * Storage provider implementation that stores assets in AWS S3 or S3-compatible
 * object storage (such as MinIO or LocalStack).
 */
@Slf4j
@Component
public class S3StorageProvider implements StorageProvider {

    private final StorageProperties properties;
    private S3Client s3Client;

    @Autowired
    public S3StorageProvider(StorageProperties properties) {
        this.properties = properties;
        log.info("S3StorageProvider initialized with bucket: {}", properties.getS3().getBucket());
    }

    /**
     * Package-private constructor for unit testing with a mock or custom S3Client.
     */
    S3StorageProvider(StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @Override
    public String name() {
        return "s3";
    }

    @Override
    public String upload(String path, InputStream content, String contentType, long size) {
        String bucket = properties.getS3().getBucket();
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .contentType(contentType)
                .contentLength(size)
                .build();

        try {
            getS3Client().putObject(putRequest, RequestBody.fromInputStream(content, size));
            log.debug("Uploaded asset to s3://{}/{}", bucket, path);
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload asset to S3: " + path, e);
        }
    }

    @Override
    public Optional<InputStream> download(String storageLocation) {
        String bucket = properties.getS3().getBucket();
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageLocation)
                .build();

        try {
            ResponseInputStream<GetObjectResponse> responseStream = getS3Client().getObject(getRequest);
            return Optional.of(responseStream);
        } catch (NoSuchKeyException e) {
            log.debug("S3 object not found: bucket={}, key={}", bucket, storageLocation);
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw new RuntimeException("Failed to download asset from S3: " + storageLocation, e);
        }
    }

    @Override
    public boolean delete(String storageLocation) {
        String bucket = properties.getS3().getBucket();
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageLocation)
                .build();

        try {
            getS3Client().deleteObject(deleteRequest);
            log.debug("Deleted asset from s3://{}/{}", bucket, storageLocation);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete asset from S3: {}", storageLocation, e);
            return false;
        }
    }

    @Override
    public boolean exists(String storageLocation) {
        String bucket = properties.getS3().getBucket();
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(storageLocation)
                .build();

        try {
            getS3Client().headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new RuntimeException("Failed to check S3 object existence: " + storageLocation, e);
        }
    }

    @Override
    public String resolve(String storageLocation) {
        String publicBaseUrl = properties.getS3().getPublicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            String key = storageLocation.startsWith("/") ? storageLocation.substring(1) : storageLocation;
            return base + "/" + key;
        }

        String bucket = properties.getS3().getBucket();
        try {
            return getS3Client().utilities().getUrl(b -> b.bucket(bucket).key(storageLocation)).toExternalForm();
        } catch (Exception e) {
            return "s3://" + bucket + "/" + storageLocation;
        }
    }

    @Override
    public boolean health() {
        String bucket = properties.getS3().getBucket();
        try {
            getS3Client().headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception e) {
            log.warn("S3 health check failed for bucket {}: {}", bucket, e.getMessage());
            return false;
        }
    }

    private synchronized S3Client getS3Client() {
        if (this.s3Client == null) {
            var s3Props = properties.getS3();
            var builder = S3Client.builder()
                    .region(Region.of(s3Props.getRegion()));

            if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isBlank()) {
                builder.endpointOverride(URI.create(s3Props.getEndpoint()));
            }

            if (s3Props.getAccessKey() != null && !s3Props.getAccessKey().isBlank()
                    && s3Props.getSecretKey() != null && !s3Props.getSecretKey().isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())));
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Props.isPathStyleAccess())
                    .build());

            this.s3Client = builder.build();
        }
        return this.s3Client;
    }
}
