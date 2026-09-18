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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ScorecardStorageProvider Unit Tests")
class S3ScorecardStorageProviderTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private ScorecardStorageProperties properties;
    private S3ScorecardStorageProvider provider;

    private static final String BUCKET_NAME = "test-scorecard-bucket";

    @BeforeEach
    void setUp() {
        properties = new ScorecardStorageProperties();
        properties.setMode("s3");
        properties.getS3().setBucket(BUCKET_NAME);
        properties.getS3().setRegion("ap-south-1");
        provider = new S3ScorecardStorageProvider(properties, s3Client, s3Presigner);
    }

    @Test
    @DisplayName("Upload stores object in S3 bucket")
    void uploadSuccess() {
        byte[] content = "%PDF-1.4 s3 scorecard".getBytes(StandardCharsets.UTF_8);
        String key = "scorecards/scorecard-1.pdf";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = provider.upload(key, new ByteArrayInputStream(content), "application/pdf", content.length);

        assertThat(result).isEqualTo(key);
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(captor.getValue().key()).isEqualTo(key);
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Download returns input stream from S3")
    void downloadSuccess() throws Exception {
        String key = "scorecards/scorecard-1.pdf";
        byte[] expectedBytes = "%PDF-1.4 content".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder().contentLength((long) expectedBytes.length).build();
        ResponseInputStream<GetObjectResponse> responseStream =
                new ResponseInputStream<>(response, new ByteArrayInputStream(expectedBytes));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        Optional<InputStream> result = provider.download(key);
        assertThat(result).isPresent();
        assertThat(new String(result.get().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("%PDF-1.4 content");
    }

    @Test
    @DisplayName("Download returns empty when object does not exist in S3")
    void downloadNotFound() {
        String key = "scorecards/missing.pdf";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("The specified key does not exist").build());

        Optional<InputStream> result = provider.download(key);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Exists returns true when HeadObject succeeds")
    void existsTrue() {
        String key = "scorecards/scorecard-1.pdf";
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertThat(provider.exists(key)).isTrue();
    }

    @Test
    @DisplayName("Exists returns false when HeadObject throws NoSuchKeyException")
    void existsFalse() {
        String key = "scorecards/scorecard-1.pdf";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThat(provider.exists(key)).isFalse();
    }

    @Test
    @DisplayName("Delete removes object from S3")
    void deleteSuccess() {
        String key = "scorecards/scorecard-1.pdf";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        boolean deleted = provider.delete(key);
        assertThat(deleted).isTrue();
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Generate presigned URL returns signed URL for download")
    void generatePresignedUrl() throws Exception {
        String key = "scorecards/scorecard-1.pdf";
        URL presignedUrl = new URL("https://test-scorecard-bucket.s3.ap-south-1.amazonaws.com/scorecards/scorecard-1.pdf?X-Amz-Signature=test");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(presignedUrl);

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = provider.generatePresignedUrl(key, Duration.ofMinutes(15));
        assertThat(url).isEqualTo(presignedUrl.toString());
    }

    @Test
    @DisplayName("Health returns true when headBucket succeeds")
    void healthCheckSuccess() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
        assertThat(provider.health()).isTrue();
    }
}
