package com.sidutti.charlie.controller;

import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.model.SearchResults;
import com.sidutti.charlie.service.*;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

//enable cors for enabling cors
@CrossOrigin(origins = "*")
@RestController
public class EmbeddingController {

    private final WikiRandomEmbeddingGenerator generator;
    private final SearchService searchService;
    private final HuggingFaceService huggingFaceService;
    private final PdfService pdfService;
    private final SplitService splitService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;

    @Autowired
    public EmbeddingController(
            WikiRandomEmbeddingGenerator generator,
            SearchService searchService,
            HuggingFaceService huggingFaceService,
            PdfService pdfService, SplitService splitService, EmbeddingService embeddingService, VectorService vectorService) {

        this.generator = generator;
        this.searchService = searchService;
        this.huggingFaceService = huggingFaceService;
        this.pdfService = pdfService;
        this.splitService = splitService;
        this.embeddingService = embeddingService;
        this.vectorService = vectorService;
    }


    @GetMapping("/ai/embedding/wiki")
    public Mono<Document> startEmbedding() {
        return generator.generateRandomEmbedding();
    }

    @GetMapping("/ai/math/embedding/start")
    public Flux<IndexResponse> startMathEmbedding(@RequestParam(value = "pageNumber", defaultValue = "10") int pageNumber,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        long start = System.currentTimeMillis();
        return huggingFaceService.createEmbeddingsFromHuggingFace(pageNumber, pageSize, "nvidia/OpenMathInstruct-1")
                .doFinally(_ -> System.out.println("Finance Embedding finished : " + (System.currentTimeMillis() - start) + "ms"));
    }

    @GetMapping("/ai/finance/embedding/start")
    public Flux<IndexResponse> startFinanceEmbedding(@RequestParam(value = "pageNumber", defaultValue = "10") int pageNumber,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        long start = System.currentTimeMillis();
        return huggingFaceService.createEmbeddingsFromHuggingFace(pageNumber, pageSize, "DeividasM/financial-instruction-aq22")
                .doFinally(_ -> System.out.println("Finance Embedding finished : " + (System.currentTimeMillis() - start) + "ms"));
    }

    @PostMapping("ai/embedding/search")
    public Flux<SearchResults> searchEmbedding(@RequestBody String query) {
        SearchRequest searchRequest = SearchRequest.defaults()
                .withSimilarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .withQuery(query)
                .withTopK(15);
        long start = System.currentTimeMillis();
        return searchService.similaritySearch(searchRequest)
                .doFinally(_ -> System.out.println("Finance Embedding finished : " + (System.currentTimeMillis() - start) + "ms"));
    }

    @PostMapping("/ai/pdf/embedding/start")
    public void chunkAndStorePDF(@RequestBody String path) throws IOException {
        path = path.replace("\"", ""); // replace backslashes with forward slashes for correct

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths.filter(Files::isRegularFile)
                    .parallel()
                    .map(pdfService::parseDocument)
                    .map(splitService::splitDocument)
                    .flatMap(Collection::parallelStream)
                    .map(embeddingService::createEmbeddedDocument)
                    .map(vectorService::saveDocument)
                    .forEach(Mono::subscribe);
        }
    }
}
