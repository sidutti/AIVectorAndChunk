package com.sidutti.charlie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.model.wiki.RandomWikiPage;
import com.sidutti.charlie.repository.elastic.ElasticDocumentRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WikiRandomEmbeddingGenerator {
    private final EmbeddingModel model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ElasticDocumentRepository documentRepository;

    public WikiRandomEmbeddingGenerator(EmbeddingModel model,
                                        WebClient webClient,
                                        ObjectMapper objectMapper,
                                        ElasticDocumentRepository documentRepository) {
        this.model = model;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;

    }

    public Mono<Document> generateRandomEmbedding() {
        return webClient
                .get()
                .uri("https://en.wikipedia.org/api/rest_v1/page/random/summary")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(Throwable::printStackTrace)
                .map(this::parseStringToJson)
                .flatMap(this::createDocument);
    }

    private RandomWikiPage parseStringToJson(String s) {
        try {
            return objectMapper.readValue(s, RandomWikiPage.class);
        } catch (JsonProcessingException e) {
            return new RandomWikiPage();
        }
    }

    private Mono<Document> createDocument(RandomWikiPage randomWikiPage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", randomWikiPage.getTitle());
        metadata.put("description", randomWikiPage.getDescription());
        float[] embedding = model.embed(randomWikiPage.getExtract());
        Document doc = new Document(UUID.randomUUID().toString(), metadata, randomWikiPage.getExtract(), embedding);
        return documentRepository.save(doc);
    }

}
