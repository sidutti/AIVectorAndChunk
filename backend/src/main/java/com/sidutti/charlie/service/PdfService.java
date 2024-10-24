package com.sidutti.charlie.service;

import com.sidutti.charlie.repository.elastic.ElasticDocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PdfService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);
    private final ElasticDocumentRepository documentRepository;
    private final EmbeddingModel model;
    private final SplitService splitService;


    public PdfService(ElasticDocumentRepository documentRepository,
                      @Qualifier("mpnetEmbedding") EmbeddingModel model,
                      SplitService splitService) {
        this.documentRepository = documentRepository;
        this.model = model;
        this.splitService = splitService;
    }

    public Document parseDocument(Path path) {
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        try (InputStream docStream = Files.newInputStream(path.toAbsolutePath())) {
            LOGGER.debug("Path to File {}", path);
            Document loadedDocument = parser.parse(docStream);
            loadedDocument.metadata().put("file", path.toAbsolutePath().toString());
            loadedDocument.metadata().put("name", path.getFileName().toString());
            return loadedDocument;
        } catch (IOException e) {
            LOGGER.error("Failed to parse", e);
        }
        return new Document("Empty");
    }

    public List<TextSegment> splitDocument(Document document) {
        return splitService.splitDocument(document);
    }

    public com.sidutti.charlie.model.Document createDocument(TextSegment segment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("file", segment.metadata().getString("file"));
        metadata.put("name", segment.metadata().getString("name"));
        var cleanText = cleanUp(segment.text());
        return new com.sidutti.charlie.model.Document(UUID.randomUUID().toString(), metadata, cleanText, model.embed(cleanText));
    }

    private String cleanUp(String text) {
        return text.replaceAll("\n", " ").replaceAll("\r\n", " ").replaceAll("\t", " ");
    }

    public Mono<com.sidutti.charlie.model.Document> saveDocument(com.sidutti.charlie.model.Document document) {
        return documentRepository.save(document);
    }
}
