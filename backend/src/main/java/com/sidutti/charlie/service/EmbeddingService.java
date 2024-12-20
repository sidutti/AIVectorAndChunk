package com.sidutti.charlie.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] createEmbedding(String text) {
        return embeddingModel.embed(text);
    }


    public Document createEmbeddedDocument(Document doc) {
        float[] embedding = createEmbedding(doc.getContent());
        doc.setEmbedding(embedding);
        return doc;
    }
}
