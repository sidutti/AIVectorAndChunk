package com.sidutti.charlie.service;

import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.sidutti.charlie.model.Root;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.net.URI;

@Component
public class HuggingFaceService {

    private final WebClient webClient;
    private final SplitService splitService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;


    public HuggingFaceService(WebClient webClient,
                              SplitService splitService, EmbeddingService embeddingService, VectorService vectorService) {
        this.webClient = webClient;
        this.splitService = splitService;
        this.embeddingService = embeddingService;
        this.vectorService = vectorService;
    }

    public Flux<IndexResponse> createEmbeddingsFromHuggingFace(int pageNumber, int numberOfRows, String dataset) {
        return webClient
                .get()
                .uri(uriBuilder ->
                        createUri(pageNumber, numberOfRows, dataset, uriBuilder))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Root.class)
                .retryWhen(RetryBackoffSpec.backoff(3, java.time.Duration.ofSeconds(10)))
                .map(Root::rows)
                .flatMapIterable(list -> list)
                .map(Root.RootRow::row)
                .map(splitService::splitDocument)
                .flatMapIterable(list -> list)
                .map(embeddingService::createEmbeddedDocument)
                .flatMap(vectorService::saveDocument)

                .onErrorResume(e -> {
                    System.out.println("Error creating document: " + e.getMessage());
                    return Mono.empty();
                });
    }

    private static URI createUri(int pageNumber, int numberOfRows, String dataset, UriBuilder uriBuilder) {
        return uriBuilder.host("datasets-server.huggingface.co")
                .scheme("https")
                .path("rows")
                .queryParam("dataset", dataset)
                .queryParam("config", "default")
                .queryParam("split", "train")
                .queryParam("offset", pageNumber)
                .queryParam("length", numberOfRows)
                .build();
    }

}
