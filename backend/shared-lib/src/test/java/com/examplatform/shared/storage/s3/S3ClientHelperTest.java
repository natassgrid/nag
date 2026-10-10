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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("S3ClientHelper Unit Tests")
class S3ClientHelperTest {

    private record TestS3Config(
            String region,
            String endpoint,
            String accessKey,
            String secretKey,
            boolean pathStyleAccess
    ) implements S3Config {
        @Override public String getRegion() { return region; }
        @Override public String getEndpoint() { return endpoint; }
        @Override public String getAccessKey() { return accessKey; }
        @Override public String getSecretKey() { return secretKey; }
        @Override public boolean isPathStyleAccess() { return pathStyleAccess; }
    }

    @Test
    @DisplayName("createS3Client builds client with static credentials and endpoint override")
    void testCreateS3ClientWithStaticCredentials() {
        S3Config config = new TestS3Config("ap-south-1", "http://localhost:9000", "minioadmin", "minioadmin", true);
        try (S3Client client = S3ClientHelper.createS3Client(config)) {
            assertThat(client).isNotNull();
            assertThat(client.serviceClientConfiguration().region().id()).isEqualTo("ap-south-1");
            assertThat(client.serviceClientConfiguration().endpointOverride()).contains(java.net.URI.create("http://localhost:9000"));
        }
    }

    @Test
    @DisplayName("createS3Client throws on null config")
    void testCreateS3ClientNullConfig() {
        assertThatThrownBy(() -> S3ClientHelper.createS3Client(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createS3Presigner builds presigner with static credentials and endpoint override")
    void testCreateS3PresignerWithStaticCredentials() {
        S3Config config = new TestS3Config("us-east-1", "http://localhost:9000", "minioadmin", "minioadmin", true);
        try (S3Presigner presigner = S3ClientHelper.createS3Presigner(config)) {
            assertThat(presigner).isNotNull();
        }
    }

    @Test
    @DisplayName("createS3Presigner throws on null config")
    void testCreateS3PresignerNullConfig() {
        assertThatThrownBy(() -> S3ClientHelper.createS3Presigner(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkBucketHealth returns true on successful headBucket call")
    void testCheckBucketHealthSuccess() {
        S3Client mockClient = mock(S3Client.class);
        when(mockClient.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());

        boolean healthy = S3ClientHelper.checkBucketHealth(mockClient, "test-bucket", "Asset");
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("checkBucketHealth returns false on exception or invalid bucket")
    void testCheckBucketHealthFailure() {
        S3Client mockClient = mock(S3Client.class);
        when(mockClient.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder().message("Bucket does not exist").build());

        boolean healthy = S3ClientHelper.checkBucketHealth(mockClient, "bad-bucket");
        assertThat(healthy).isFalse();

        assertThat(S3ClientHelper.checkBucketHealth(null, "bucket")).isFalse();
        assertThat(S3ClientHelper.checkBucketHealth(mockClient, null)).isFalse();
        assertThat(S3ClientHelper.checkBucketHealth(mockClient, "   ")).isFalse();
    }
}
