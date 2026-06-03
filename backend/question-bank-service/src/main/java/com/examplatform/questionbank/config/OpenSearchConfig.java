package com.examplatform.questionbank.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenSearch Java client for full-text search of the question bank.
 * Supports 100M question capacity with sub-2-second p95 query response.
 *
 * Validates: Requirements 19.3
 */
@Configuration
public class OpenSearchConfig {

    @Value("${app.opensearch.host}")
    private String host;

    @Value("${app.opensearch.port}")
    private int port;

    @Value("${app.opensearch.scheme}")
    private String scheme;

    @Bean
    public RestClient openSearchRestClient() {
        return RestClient.builder(new HttpHost(host, port, scheme)).build();
    }

    @Bean
    public OpenSearchClient openSearchClient(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
