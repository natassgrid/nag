/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.shared.storage.s3;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Shared utility for configuring and instantiating AWS S3 clients and presigners,
 * and performing bucket connectivity health checks.
 *
 * @author Natassia Grid Development Team
 * @since 1.0.0
 */
@Slf4j
public final class S3ClientHelper {

    private static final String DEFAULT_REGION = "ap-south-1";

    private S3ClientHelper() {
        // Utility class
    }

    /**
     * Builds a configured {@link S3Client} using an {@link S3Config}.
     *
     * @param config S3 configuration provider
     * @return a newly configured S3Client instance
     */
    public static S3Client createS3Client(S3Config config) {
        if (config == null) {
            throw new IllegalArgumentException("S3Config must not be null");
        }
        return createS3Client(
                config.getRegion(),
                config.getEndpoint(),
                config.getAccessKey(),
                config.getSecretKey(),
                config.isPathStyleAccess()
        );
    }

    /**
     * Builds a configured {@link S3Client} from individual parameters.
     *
     * @param region AWS region (defaults to ap-south-1 if null or blank)
     * @param endpoint optional custom endpoint override (e.g. MinIO)
     * @param accessKey optional AWS access key
     * @param secretKey optional AWS secret key
     * @param pathStyleAccess whether to enable path-style access
     * @return a newly configured S3Client instance
     */
    public static S3Client createS3Client(
            String region,
            String endpoint,
            String accessKey,
            String secretKey,
            boolean pathStyleAccess) {

        String resolvedRegion = (region != null && !region.isBlank()) ? region : DEFAULT_REGION;
        var builder = S3Client.builder()
                .region(Region.of(resolvedRegion));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build());

        return builder.build();
    }

    /**
     * Builds a configured {@link S3Presigner} using an {@link S3Config}.
     *
     * @param config S3 configuration provider
     * @return a newly configured S3Presigner instance
     */
    public static S3Presigner createS3Presigner(S3Config config) {
        if (config == null) {
            throw new IllegalArgumentException("S3Config must not be null");
        }
        return createS3Presigner(
                config.getRegion(),
                config.getEndpoint(),
                config.getAccessKey(),
                config.getSecretKey(),
                config.isPathStyleAccess()
        );
    }

    /**
     * Builds a configured {@link S3Presigner} from individual parameters.
     *
     * @param region AWS region (defaults to ap-south-1 if null or blank)
     * @param endpoint optional custom endpoint override (e.g. MinIO)
     * @param accessKey optional AWS access key
     * @param secretKey optional AWS secret key
     * @param pathStyleAccess whether to enable path-style access
     * @return a newly configured S3Presigner instance
     */
    public static S3Presigner createS3Presigner(
            String region,
            String endpoint,
            String accessKey,
            String secretKey,
            boolean pathStyleAccess) {

        String resolvedRegion = (region != null && !region.isBlank()) ? region : DEFAULT_REGION;
        var builder = S3Presigner.builder()
                .region(Region.of(resolvedRegion));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build());

        return builder.build();
    }

    /**
     * Executes a bucket health check using {@code headBucket}.
     *
     * @param s3Client the S3 client to query
     * @param bucket the target bucket name
     * @return true if the bucket is reachable and healthy, false otherwise
     */
    public static boolean checkBucketHealth(S3Client s3Client, String bucket) {
        return checkBucketHealth(s3Client, bucket, null);
    }

    /**
     * Executes a bucket health check using {@code headBucket} with optional logging prefix.
     *
     * @param s3Client the S3 client to query
     * @param bucket the target bucket name
     * @param serviceName optional service label for log diagnostics
     * @return true if the bucket is reachable and healthy, false otherwise
     */
    public static boolean checkBucketHealth(S3Client s3Client, String bucket, String serviceName) {
        if (s3Client == null || bucket == null || bucket.isBlank()) {
            return false;
        }
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception e) {
            String label = (serviceName != null && !serviceName.isBlank()) ? serviceName + " " : "";
            log.warn("{}S3 health check failed for bucket {}: {}", label, bucket, e.getMessage());
            return false;
        }
    }
}
