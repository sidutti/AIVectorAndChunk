package com.sidutti.charlie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.model.wiki.RandomWikiPage;
import com.sidutti.charlie.repository.DocumentRepository;
import com.sidutti.charlie.vector.mongo.MongoDBAtlasVectorStore;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WikiRandomEmbeddingGenerator {
        private final MongoDBAtlasVectorStore vectorStore;
        private final WebClient webClient;
        private final ObjectMapper objectMapper;
        private final DocumentRepository documentRepository;

        public WikiRandomEmbeddingGenerator(MongoDBAtlasVectorStore vectorStore, WebClient webClient, ObjectMapper objectMapper, DocumentRepository documentRepository) {
                this.vectorStore = vectorStore;
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
                org.springframework.ai.document.Document document = new org.springframework.ai.document.Document(randomWikiPage.getExtract(), metadata);
                List<Double> embedding = vectorStore.generateEmbeddings(document);
                Document doc = new Document(new ObjectId(), metadata, randomWikiPage.getExtract(), embedding);
                return documentRepository.save(doc);
        }

}
