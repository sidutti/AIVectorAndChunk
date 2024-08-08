package com.sidutti.charlie.service;

import com.sidutti.charlie.repository.elastic.ElasticDocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class PdfService {
    private final ElasticDocumentRepository documentRepository;
    private final EmbeddingModel model;
    private final SplitService splitService;

    public PdfService(ElasticDocumentRepository documentRepository, EmbeddingModel model, SplitService splitService) {
        this.documentRepository = documentRepository;
        this.model = model;
        this.splitService = splitService;
    }

    public Stream<Document> parsePdf(String filePath) throws IOException {
    try (Stream<Path> paths = Files.walk(Paths.get(filePath))) {
        return paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".pdf"))
                   .map(this::parseDocument);
    }
}

    private Document parseDocument(Path path)   {
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

        try {
            return parser.parse(Files.newInputStream(path.toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TextSegment> splitDocument(Document document) {
        return splitService.splitDocument(document);
    }
    public com.sidutti.charlie.model.Document createDocument(TextSegment segment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title",segment.metadata().getString("title"));
        metadata.put("description", segment.metadata().getString("description"));
        List<Double> embedding = model.embed(segment.text());
        return new com.sidutti.charlie.model.Document(UUID.randomUUID().toString(), metadata, segment.text(), embedding);
    }
    public Mono<com.sidutti.charlie.model.Document> saveDocument(com.sidutti.charlie.model.Document document) {
        return documentRepository.save(document);
    }
}
