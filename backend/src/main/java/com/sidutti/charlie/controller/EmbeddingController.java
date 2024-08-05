package com.sidutti.charlie.controller;

import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.model.SearchResults;
import com.sidutti.charlie.service.HuggingFaceService;
import com.sidutti.charlie.service.SearchService;
import com.sidutti.charlie.service.WikiRandomEmbeddingGenerator;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

//enable cors for enabling cors
@CrossOrigin(origins = "*")
@RestController
public class EmbeddingController {
    private final EmbeddingModel embeddingModel;
    private final WikiRandomEmbeddingGenerator generator;
    private final SearchService searchService;
    private final HuggingFaceService huggingFaceService;


    @Autowired
    public EmbeddingController(EmbeddingModel embeddingModel, WikiRandomEmbeddingGenerator generator, SearchService searchService, HuggingFaceService huggingFaceService) {
        this.embeddingModel = embeddingModel;
        this.generator = generator;
        this.searchService = searchService;
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
        long start = System.currentTimeMillis();
        return huggingFaceService.createEmbeddingsFromHuggingFace(pageNumber, pageSize, "ibivibiv/math_instruct")
                .doFinally(a -> System.out.println("Finance Embedding finished : " + (System.currentTimeMillis() - start) + "ms"));
    }

    @GetMapping("/ai/finance/embedding/start")
    public Flux<Document> startFinanceEmbedding(@RequestParam(value = "pageNumber", defaultValue = "10") int pageNumber,
                                                @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        long start = System.currentTimeMillis();
        return huggingFaceService.createEmbeddingsFromHuggingFace(pageNumber, pageSize, "DeividasM/financial-instruction-aq22")
                .doFinally(a -> System.out.println("Finance Embedding finished : " + (System.currentTimeMillis() - start) + "ms"));
    }

    @PostMapping("ai/embedding/search")
    public Flux<SearchResults> searchEmbedding(@RequestBody String query) {
        SearchRequest searchRequest = SearchRequest.defaults()
                .withQuery(query)
                .withTopK(10)
                .withSimilarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL);
        long start = System.currentTimeMillis();
        return searchService.similaritySearch(searchRequest)
                .doFinally(a -> System.out.println("Finance Embedding finished : " + (System.currentTimeMillis() - start) + "ms"));
    }


}
