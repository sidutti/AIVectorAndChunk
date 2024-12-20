package com.sidutti.charlie.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.vertexai.VertexAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiConnectionProperties;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.List;

@Configuration
public class AIConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIConfig.class);

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(true)
                ))
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(2048 * 1024))
                .build();
    }

    @Bean
    public RestClient.Builder ollamaRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        LOGGER.info("Creating ObjectMapper");
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
                .registerModule(new JavaTimeModule());
    }

    @Bean
    public ElasticsearchAsyncClient elasticsearchAsyncClient(org.elasticsearch.client.RestClient restClient, ObjectMapper objectMapper) {
        LOGGER.info("Creating ElasticsearchAsyncClient");
        return new ElasticsearchAsyncClient(new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper)));
    }

    @Bean
    public VertexAI vertexAi(VertexAiGeminiConnectionProperties connectionProperties) throws IOException {
        LOGGER.info("Creating vertexAi");
        Assert.hasText(connectionProperties.getProjectId(), "Vertex AI project-id must be set!");
        Assert.hasText(connectionProperties.getLocation(), "Vertex AI location must be set!");
        Assert.notNull(connectionProperties.getTransport(), "Vertex AI transport must be set!");

        var vertexAIBuilder = new VertexAI.Builder().setProjectId(connectionProperties.getProjectId())
                .setLocation(connectionProperties.getLocation())
                .setTransport(com.google.cloud.vertexai.Transport.valueOf(connectionProperties.getTransport().name()));

        if (StringUtils.hasText(connectionProperties.getApiEndpoint())) {
            vertexAIBuilder.setApiEndpoint(connectionProperties.getApiEndpoint());
        }
        if (!CollectionUtils.isEmpty(connectionProperties.getScopes())) {
            vertexAIBuilder.setScopes(connectionProperties.getScopes());
        }

        if (connectionProperties.getCredentialsUri() != null) {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(connectionProperties.getCredentialsUri().getInputStream())
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            vertexAIBuilder.setCredentials(credentials);
        }
        return vertexAIBuilder.build();
    }
    @Bean
    public VertexAiEmbeddingConnectionDetails createConnection(VertexAiGeminiConnectionProperties connectionProperties) throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(connectionProperties.getCredentialsUri().getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        PredictionServiceSettings predictionServiceSettings = PredictionServiceSettings.newBuilder()
                .setEndpoint(connectionProperties.getApiEndpoint())
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return VertexAiEmbeddingConnectionDetails.builder()
                .withProjectId(connectionProperties.getProjectId())
                .withLocation(connectionProperties.getLocation())
                .withApiEndpoint(connectionProperties.getApiEndpoint())
                .withPredictionServiceSettings(predictionServiceSettings)
                .build();
    }
}
