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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Storage provider implementation that stores scorecard PDFs in AWS S3 or
 * S3-compatible object storage (such as MinIO or LocalStack).
 */
@Slf4j
@Component
public class S3ScorecardStorageProvider implements ScorecardStorageProvider {

    private final ScorecardStorageProperties properties;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @Autowired
    public S3ScorecardStorageProvider(ScorecardStorageProperties properties) {
        this.properties = properties;
        log.info("S3ScorecardStorageProvider initialized with bucket: {}", properties.getEffectiveS3Bucket());
    }

    /**
     * Package-private constructor for unit testing with mock S3Client and S3Presigner.
     */
    S3ScorecardStorageProvider(ScorecardStorageProperties properties, S3Client s3Client, S3Presigner s3Presigner) {
        this.properties = properties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String name() {
        return "s3";
    }

    @Override
    public String upload(String path, InputStream content, String contentType, long size) {
        String bucket = properties.getEffectiveS3Bucket();
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .contentType(contentType != null ? contentType : "application/pdf")
                .contentLength(size)
                .build();

        try {
            getS3Client().putObject(putRequest, RequestBody.fromInputStream(content, size));
            log.debug("Uploaded scorecard PDF to s3://{}/{}", bucket, path);
            return path;
        } catch (Exception e) {
            log.error("Failed to upload scorecard PDF to S3 at path {}: {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to upload scorecard to S3: " + path, e);
        }
    }

    @Override
    public Optional<InputStream> download(String storageLocation) {
        String bucket = properties.getEffectiveS3Bucket();
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageLocation)
                .build();

        try {
            ResponseInputStream<GetObjectResponse> responseStream = getS3Client().getObject(getRequest);
            return Optional.of(responseStream);
        } catch (NoSuchKeyException e) {
            log.debug("S3 scorecard object not found: bucket={}, key={}", bucket, storageLocation);
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.error("Failed to download scorecard from S3: bucket={}, key={}, error={}",
                    bucket, storageLocation, e.getMessage(), e);
            throw new RuntimeException("Failed to download scorecard from S3: " + storageLocation, e);
        }
    }

    @Override
    public boolean delete(String storageLocation) {
        String bucket = properties.getEffectiveS3Bucket();
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageLocation)
                .build();

        try {
            getS3Client().deleteObject(deleteRequest);
            log.debug("Deleted scorecard from s3://{}/{}", bucket, storageLocation);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete scorecard from S3: {}", storageLocation, e);
            return false;
        }
    }

    @Override
    public boolean exists(String storageLocation) {
        String bucket = properties.getEffectiveS3Bucket();
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
            throw new RuntimeException("Failed to check S3 scorecard existence: " + storageLocation, e);
        }
    }

    @Override
    public String generatePresignedUrl(String storageLocation, Duration duration) {
        String bucket = properties.getEffectiveS3Bucket();
        Duration effectiveDuration = (duration != null && !duration.isZero() && !duration.isNegative())
                ? duration
                : Duration.ofMinutes(properties.getS3().getPresignedUrlDurationMinutes());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageLocation)
                .responseContentDisposition("attachment; filename=\"scorecard.pdf\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(effectiveDuration)
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            PresignedGetObjectRequest presignedGetObjectRequest = getS3Presigner().presignGetObject(presignRequest);
            return presignedGetObjectRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for S3 scorecard key {}: {}", storageLocation, e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned scorecard URL", e);
        }
    }

    @Override
    public boolean health() {
        String bucket = properties.getEffectiveS3Bucket();
        try {
            getS3Client().headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception e) {
            log.warn("S3 scorecard health check failed for bucket {}: {}", bucket, e.getMessage());
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

    private synchronized S3Presigner getS3Presigner() {
        if (this.s3Presigner == null) {
            var s3Props = properties.getS3();
            var builder = S3Presigner.builder()
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

            this.s3Presigner = builder.build();
        }
        return this.s3Presigner;
    }
}
