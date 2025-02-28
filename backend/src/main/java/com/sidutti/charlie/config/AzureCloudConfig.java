package com.sidutti.charlie.config;

import com.azure.ai.documentintelligence.DocumentIntelligenceAsyncClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.core.credential.KeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureCloudConfig {

    @Bean
    public DocumentIntelligenceAsyncClient azureDocumentClient(@Value("${cloud.microsoft.endpoint}") String endpoint,
                                                        @Value("${cloud.microsoft.key}") String key) {
        return new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(new KeyCredential(key))
                .buildAsyncClient();
    }
}
