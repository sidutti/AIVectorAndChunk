package com.sidutti.charlie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class AIConfig {
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
                return new ObjectMapper();
        }
}
