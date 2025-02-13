package com.sidutti.charlie.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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
                                                                         GoogleCredentials creds) throws IOException {
        String endpoint = String.format("%s-documentai.googleapis.com:443", location);
        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings
                .newBuilder()
                .setEndpoint(endpoint)
                .setCredentialsProvider(FixedCredentialsProvider.create(creds))
                .build();
        return DocumentProcessorServiceClient.create(settings);
    }

    @Bean
    public GoogleCredentials getGoogleCredentials(@Value("${cloud.google.cred-path}") String credPath) throws IOException {
        return GoogleCredentials.fromStream(new FileInputStream(credPath))
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
    }

    @Bean("googleCloudRequestName")
    public String googleCloudRequestName(@Value("${cloud.google.project-id}") String projectId,
                                         @Value("${cloud.google.location}") String location,
                                         @Value("${cloud.google.layout-processer-id}") String processerId) {
        return String.format("projects/%s/locations/%s/processors/%s", projectId, location, processerId);
    }

    @Bean
    public Storage googleCloudStorage(@Value("${cloud.google.project-id}") String projectId,
                                      GoogleCredentials creds) {
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(creds)
                .build()
                .getService();
    }
}