package com.sidutti.charlie.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Mono<float[]> createEmbedding(String text) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(() -> embeddingModel.embed(text)));
    }

    public Mono<ElasticsearchVectorStore.ElasticSearchDocument> createEmbeddedDocument(Document document) {
        return createEmbedding(document.getText())
                .map(embed -> new ElasticsearchVectorStore.ElasticSearchDocument(document.getId(), Objects.requireNonNull(document.getText()),
                        document.getMetadata(), embed));
    }
}
