package com.sidutti.charlie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.autoconfigure.ollama.OllamaChatProperties;
import org.springframework.ai.autoconfigure.ollama.OllamaConnectionDetails;
import org.springframework.ai.autoconfigure.ollama.OllamaConnectionProperties;
import org.springframework.ai.autoconfigure.ollama.OllamaEmbeddingProperties;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({OllamaChatProperties.class, OllamaEmbeddingProperties.class,
                OllamaConnectionProperties.class})
public class AIConfig {
        @Bean
        public WebClient webClient() {
                return WebClient.builder()
                                .clientConnector(new ReactorClientHttpConnector(
                                                HttpClient.create().followRedirect(true)
                                )).build();
        }

        @Bean
        public AIConfig.PropertiesOllamaConnectionDetails ollamaConnectionDetails(OllamaConnectionProperties properties) {
                return new AIConfig.PropertiesOllamaConnectionDetails(properties);
        }

        @Bean
        public RestClient.Builder ollamaRestClientBuilder() {
                return RestClient.builder();
        }

        @Bean
        public OllamaApi ollamaApi(OllamaConnectionDetails connectionDetails, RestClient.Builder restClientBuilder) {
                return new OllamaApi(connectionDetails.getBaseUrl(), restClientBuilder);
        }

        @Bean
        public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi, OllamaChatProperties properties) {
                return new OllamaChatModel(ollamaApi, properties.getOptions());
        }

        @Bean
        public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi, OllamaEmbeddingProperties properties) {

                return new OllamaEmbeddingModel(ollamaApi, properties.getOptions());
        }

        private static class PropertiesOllamaConnectionDetails implements OllamaConnectionDetails {

                private final OllamaConnectionProperties properties;

                PropertiesOllamaConnectionDetails(OllamaConnectionProperties properties) {
                        this.properties = properties;
                }

                @Override
                public String getBaseUrl() {
                        return this.properties.getBaseUrl();
                }

        }
     @Bean
        public ObjectMapper objectMapper(){
                return new ObjectMapper();
     }
}
