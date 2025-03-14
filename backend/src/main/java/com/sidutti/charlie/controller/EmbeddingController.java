package com.sidutti.charlie.controller;

import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.sidutti.charlie.model.SearchResults;
import com.sidutti.charlie.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;


//enable cors for enabling cors
@CrossOrigin(origins = "*")
@RestController
public class EmbeddingController {
    private final Logger LOGGER = LoggerFactory.getLogger(EmbeddingController.class);


    private final ElasticSearchService elasticSearchService;
    private final HuggingFaceService huggingFaceService;
    private final PdfService pdfService;
    private final SplitService splitService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final WebClient client;

    @Autowired
    public EmbeddingController(

            ElasticSearchService elasticSearchService,
            HuggingFaceService huggingFaceService,
            PdfService pdfService, SplitService splitService,
            EmbeddingService embeddingService,
            VectorService vectorService,
            WebClient client) {
        this.elasticSearchService = elasticSearchService;
        this.huggingFaceService = huggingFaceService;
        this.pdfService = pdfService;
        this.splitService = splitService;
        this.embeddingService = embeddingService;
        this.vectorService = vectorService;
        this.client = client;
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
        SearchRequest searchRequest = SearchRequest.builder()
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .query(query)
                .topK(15)
                .build();
        long start = System.currentTimeMillis();
        return elasticSearchService.similaritySearch(searchRequest)
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

    @PostMapping("/ai/irs/embedding/start")
    public void chunkAndStoreIRS() {

        try (InputStream resourceAsStream = new ClassPathResource("irslist.txt").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //sleep for 5 sec

                client
                        .get()
                        .uri(line)
                        .retrieve()
                        .bodyToFlux(DataBuffer.class)
                        .retryWhen(RetryBackoffSpec.backoff(3, Duration.ofSeconds(10)))
                        .reduce(InputStream.nullInputStream(), (s, d)
                                -> new SequenceInputStream(s, d.asInputStream(true)))
                        .map(InputStreamResource::new)
                        .map(pdfService::parseDocument)
                        .map(splitService::splitDocument)
                        .flatMapIterable(list -> list)
                        .map(embeddingService::createEmbeddedDocument)
                        .map(vectorService::saveDocument)
                        .flatMap(mono -> mono)
                        .subscribe();

            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


}
