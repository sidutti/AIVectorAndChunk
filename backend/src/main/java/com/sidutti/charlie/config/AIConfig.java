package com.sidutti.charlie.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.azure.ai.documentintelligence.DocumentIntelligenceAsyncClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import io.micrometer.observation.ObservationRegistry;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreProperties;
import org.springframework.ai.autoconfigure.vectorstore.neo4j.Neo4jVectorStoreProperties;
import org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiConnectionProperties;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean("neo4jVectorStore")
    public Neo4jVectorStore neo4jVectorStore(Driver driver,
                                        @Qualifier("azureOpenAiEmbeddingModel") EmbeddingModel embeddingModel,
                                        Neo4jVectorStoreProperties properties,
                                        ObjectProvider<ObservationRegistry> observationRegistry,
                                        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                        BatchingStrategy batchingStrategy) {

        return Neo4jVectorStore.builder(driver, embeddingModel)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .databaseName(properties.getDatabaseName())
                .embeddingDimension(properties.getEmbeddingDimension())
                .distanceType(properties.getDistanceType())
                .label(properties.getLabel())
                .embeddingProperty(properties.getEmbeddingProperty())
                .indexName(properties.getIndexName())
                .idProperty(properties.getIdProperty())
                .constraintName(properties.getConstraintName())
                .build();
    }

    @Bean
    ElasticsearchVectorStore elasticsearchVectorStore(ElasticsearchVectorStoreProperties properties,
                                         org.elasticsearch.client.RestClient restClient,
                                         @Qualifier("azureOpenAiEmbeddingModel") EmbeddingModel embeddingModel,
                                         ObjectProvider<ObservationRegistry> observationRegistry,
                                         ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                         BatchingStrategy batchingStrategy) {
        ElasticsearchVectorStoreOptions elasticsearchVectorStoreOptions = new ElasticsearchVectorStoreOptions();

        if (StringUtils.hasText(properties.getIndexName())) {
            elasticsearchVectorStoreOptions.setIndexName(properties.getIndexName());
        }
        if (properties.getDimensions() != null) {
            elasticsearchVectorStoreOptions.setDimensions(properties.getDimensions());
        }
        if (properties.getSimilarity() != null) {
            elasticsearchVectorStoreOptions.setSimilarity(properties.getSimilarity());
        }

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(elasticsearchVectorStoreOptions)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .build();
    }
}
