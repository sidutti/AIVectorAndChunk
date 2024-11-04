package com.sidutti.charlie.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Configuration
public class GoogleCloudConfig {


    @Bean
    public DocumentProcessorServiceClient documentProcessorServiceClient(@Value("${cloud.google.location}") String location,
                                                                         @Value("${cloud.google.cred-path}") String credPath) throws IOException {
        String endpoint = String.format("%s-documentai.googleapis.com:443", location);
        GoogleCredentials creds = GoogleCredentials.fromStream(new FileInputStream(credPath))
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings
                .newBuilder()
                .setEndpoint(endpoint)
                .setCredentialsProvider(FixedCredentialsProvider.create(creds))
                .build();
        return DocumentProcessorServiceClient.create(settings);
    }

    @Bean("googleCloudRequestName")
    public String googleCloudRequestName(@Value("${cloud.google.project-id}") String projectId,
                                         @Value("${cloud.google.location}") String location,
                                         @Value("${cloud.google.processer-id}") String processerId) {
        return String.format("projects/%s/locations/%s/processors/%s", projectId, location, processerId);
    }

}
