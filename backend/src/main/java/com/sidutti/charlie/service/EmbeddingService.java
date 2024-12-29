package com.sidutti.charlie.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] createEmbedding(String text) {
        return embeddingModel.embed(text);
    }


    public ElasticsearchVectorStore.ElasticSearchDocument createEmbeddedDocument(Document document) {
        float[] embedding = createEmbedding(document.getText());
        return new ElasticsearchVectorStore.ElasticSearchDocument(document.getId(), Objects.requireNonNull(document.getText()),
                document.getMetadata(), embedding);
    }
}
