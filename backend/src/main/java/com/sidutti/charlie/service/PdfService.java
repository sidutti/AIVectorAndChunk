package com.sidutti.charlie.service;

import com.sidutti.charlie.repository.elastic.ElasticDocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class PdfService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);
    private final ElasticDocumentRepository documentRepository;
    private final EmbeddingModel model;
    private final SplitService splitService;


    public PdfService(ElasticDocumentRepository documentRepository, EmbeddingModel model, SplitService splitService ) {
        this.documentRepository = documentRepository;
        this.model = model;
        this.splitService = splitService;

    }

    public Document parseDocument(Path path) {
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        try {
            LOGGER.debug("Path to File {}", path);
            return parser.parse(Files.newInputStream(path.toAbsolutePath()));
        } catch (IOException e) {
            LOGGER.error("Failed to parse",e);
        }
        return new Document("Empty");
    }

    public List<TextSegment> splitDocument(Document document) {
        List<String> chunks = splitService.chunkText(document.text());
        List<TextSegment> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if(!chunks.get(i).isBlank()) {
                result.add(new TextSegment(chunks.get(i), new Metadata(Map.of("index", i))));
            }
        }
        return result;

    }

    public com.sidutti.charlie.model.Document createDocument(TextSegment segment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", segment.metadata().getString("title"));
        metadata.put("description", segment.metadata().getString("description"));
        float[] embedding = model.embed(segment.text());
        return new com.sidutti.charlie.model.Document(UUID.randomUUID().toString(), metadata, segment.text(), embedding);
    }

    public Mono<com.sidutti.charlie.model.Document> saveDocument(com.sidutti.charlie.model.Document document) {
        return documentRepository.save(document);
    }
}
