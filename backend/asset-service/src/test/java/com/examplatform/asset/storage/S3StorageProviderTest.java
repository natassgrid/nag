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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.asset.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link S3StorageProvider}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageProvider")
class S3StorageProviderTest {

    @Mock
    private S3Client s3Client;

    private StorageProperties properties;
    private S3StorageProvider provider;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.getS3().setBucket("test-bucket");
        properties.getS3().setRegion("ap-south-1");
        provider = new S3StorageProvider(properties, s3Client);
    }

    @Test
    @DisplayName("name() returns s3")
    void nameReturnsS3() {
        assertThat(provider.name()).isEqualTo("s3");
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads object to configured bucket")
        void uploadsObject() {
            String path = "tenant1/images/pic.png";
            byte[] content = "image data".getBytes();

            String result = provider.upload(path, new ByteArrayInputStream(content), "image/png", content.length);

            assertThat(result).isEqualTo(path);
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("returns input stream when object exists")
        void returnsStreamWhenExists() {
            String path = "tenant1/images/pic.png";
            GetObjectResponse response = GetObjectResponse.builder().contentLength(10L).build();
            ResponseInputStream<GetObjectResponse> responseStream =
                    new ResponseInputStream<>(response, new ByteArrayInputStream(new byte[10]));

            when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

            Optional<InputStream> result = provider.download(path);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("returns empty when object does not exist")
        void returnsEmptyWhenMissing() {
            String path = "missing.png";
            when(s3Client.getObject(any(GetObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().message("Key does not exist").build());

            Optional<InputStream> result = provider.download(path);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes object from S3")
        void deletesObject() {
            String path = "tenant1/images/pic.png";

            boolean result = provider.delete(path);

            assertThat(result).isTrue();
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true when headObject succeeds")
        void returnsTrueWhenExists() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenReturn(HeadObjectResponse.builder().build());

            assertThat(provider.exists("tenant1/photo.jpg")).isTrue();
        }

        @Test
        @DisplayName("returns false when NoSuchKeyException thrown")
        void returnsFalseWhenMissing() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().build());

            assertThat(provider.exists("missing.jpg")).isFalse();
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("resolves using publicBaseUrl if configured")
        void resolvesWithPublicBaseUrl() {
            properties.getS3().setPublicBaseUrl("https://cdn.examplatform.org/assets");

            String url = provider.resolve("2024/01/diagram.svg");

            assertThat(url).isEqualTo("https://cdn.examplatform.org/assets/2024/01/diagram.svg");
        }

        @Test
        @DisplayName("resolves to s3 uri fallback if no publicBaseUrl and utilities fail")
        void resolvesToS3UriFallback() {
            properties.getS3().setPublicBaseUrl(null);
            String url = provider.resolve("2024/01/diagram.svg");
            assertThat(url).contains("diagram.svg");
        }
    }

    @Nested
    @DisplayName("health")
    class Health {

        @Test
        @DisplayName("returns true when headBucket succeeds")
        void returnsTrueWhenHealthy() {
            when(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .thenReturn(HeadBucketResponse.builder().build());

            assertThat(provider.health()).isTrue();
        }

        @Test
        @DisplayName("returns false when headBucket throws exception")
        void returnsFalseWhenUnhealthy() {
            when(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .thenThrow(S3Exception.builder().message("Access denied").build());

            assertThat(provider.health()).isFalse();
        }
    }
}
