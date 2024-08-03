package com.sidutti.charlie.controller;

import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.service.HuggingFaceService;
import com.sidutti.charlie.service.WikiRandomEmbeddingGenerator;
import com.sidutti.charlie.vector.mongo.MongoDBAtlasVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class EmbeddingController {
        private final EmbeddingModel embeddingModel;
        private final WikiRandomEmbeddingGenerator generator;
        private final MongoDBAtlasVectorStore vectorStore;
private final HuggingFaceService huggingFaceService;
        @Autowired
        public EmbeddingController(EmbeddingModel embeddingModel, WikiRandomEmbeddingGenerator generator, MongoDBAtlasVectorStore vectorStore, HuggingFaceService huggingFaceService) {
                this.embeddingModel = embeddingModel;
                this.generator = generator;
                this.vectorStore = vectorStore;
                this.huggingFaceService = huggingFaceService;
        }

        @GetMapping("/ai/embedding")
        public Map<String, Object> embed(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
                EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
                return Map.of("embedding", embeddingResponse);
        }

        @GetMapping("/ai/mongo/collection/create")
        public void createCollection() {
                vectorStore.createIndex();
        }

        @GetMapping("/ai/embedding/start")
        public Flux<Document> startEmbedding(@RequestParam(value = "numberOfRandomPages", defaultValue = "10") int randomPages) {
                List<Mono<Document>> mono = new ArrayList<>();
                for (int i = 0; i < randomPages; i++) {
                        mono.add(generator.generateRandomEmbedding());
                }
                return Flux.concat(mono);
        }
        @GetMapping("/ai/math/embedding/start")
        public Flux<Document> startMathEmbedding(@RequestParam(value = "pageNumber", defaultValue = "10") int pageNumber,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
                return huggingFaceService.createEmbeddingsFromHuggingFace(pageNumber,pageSize);
        }

}
