/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS SDK configuration for Bedrock Batch Inference and S3.
 * Uses static credentials when configured, otherwise falls back to the
 * default AWS credential chain (env vars, instance profile, etc.).
 */
@Configuration
public class AwsBedrockConfig {

    @Value("${app.ai.bedrock.aws-region:us-east-1}")
    private String awsRegion;

    @Value("${app.ai.bedrock.aws-access-key-id:}")
    private String accessKeyId;

    @Value("${app.ai.bedrock.aws-secret-access-key:}")
    private String secretAccessKey;

    @Bean
    public BedrockClient bedrockClient() {
        var builder = BedrockClient.builder()
                .region(Region.of(awsRegion));

        if (hasStaticCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(awsRegion));

        if (hasStaticCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    private boolean hasStaticCredentials() {
        return accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank();
    }
}
