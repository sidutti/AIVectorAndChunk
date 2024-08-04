package com.sidutti.charlie.controller;

import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.service.HuggingFaceService;
import com.sidutti.charlie.service.WikiRandomEmbeddingGenerator;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class EmbeddingController {
        private final EmbeddingModel embeddingModel;
        private final WikiRandomEmbeddingGenerator generator;

        private final HuggingFaceService huggingFaceService;


        @Autowired
        public EmbeddingController(EmbeddingModel embeddingModel, WikiRandomEmbeddingGenerator generator,HuggingFaceService huggingFaceService) {
                this.embeddingModel = embeddingModel;
                this.generator = generator;
                this.huggingFaceService = huggingFaceService;
        }

        @GetMapping("/ai/embedding")
        public Map<String, Object> embed(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
                EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
                return Map.of("embedding", embeddingResponse);
        }

        @GetMapping("/ai/embedding/wiki")
        public Mono<Document> startEmbedding() {
                return generator.generateRandomEmbedding();
        }

        @GetMapping("/ai/math/embedding/start")
        public Flux<Document> startMathEmbedding(@RequestParam(value = "pageNumber", defaultValue = "10") int pageNumber,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
                return huggingFaceService.createEmbeddingsFromHuggingFace(pageNumber, pageSize);
        }

}
