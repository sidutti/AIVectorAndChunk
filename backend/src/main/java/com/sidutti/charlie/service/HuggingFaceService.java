package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.model.Root;
import com.sidutti.charlie.repository.elastic.ElasticDocumentRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HuggingFaceService {
        private final EmbeddingModel model;
        private final WebClient webClient;

        private final ElasticDocumentRepository documentRepository;

        public HuggingFaceService(EmbeddingModel model, WebClient webClient, ElasticDocumentRepository documentRepository) {
                this.model = model;
                this.webClient = webClient;
                this.documentRepository = documentRepository;
        }

        public Flux<Document> createEmbeddingsFromHuggingFace(int pageNumber, int numberOfRows, String dataset) {
                return webClient
                                .get()
                                .uri(uriBuilder ->

                                                uriBuilder.host("datasets-server.huggingface.co")
                                                                .scheme("https")
                                                                .path("rows")
                                                                .queryParam("dataset", dataset)
                                                                .queryParam("config", "default")
                                                                .queryParam("split", "train")
                                                                .queryParam("offset", pageNumber)
                                                                .queryParam("length", numberOfRows)
                                                                .build())
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(Root.class)
                                .map(Root::rows)
                                .flatMapIterable(list -> list)
                                .doOnError(Throwable::printStackTrace)
                                .map(this::createDocument)
                                .flatMap(documentRepository::save)
                                .onErrorResume(e-> Mono.just(new Document("",null,"",null)));
        }


        private Document createDocument(Root.RootRow rootRow) {
                String finalValue = rootRow.row().instruction().concat(rootRow.row().output());
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", rootRow.row().output());
                metadata.put("description", rootRow.row().instruction());
                List<Double> embedding = model.embed(finalValue);
                return new Document(UUID.randomUUID().toString(), metadata, finalValue, embedding);
        }
}
