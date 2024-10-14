package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Root;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class SplitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SplitService.class);
    private final DocumentSplitter splitter;
    private final SemanticChunkingService semanticChunkingService;

    public SplitService(SemanticChunkingService semanticChunkingService) {
        this.semanticChunkingService = semanticChunkingService;
        this.splitter = DocumentSplitters.recursive(600, 30, new HuggingFaceTokenizer());
    }

    public List<TextSegment> splitDocument(Root.Row row) {
        try {
            String finalValue = row.instruction().concat(row.output());
            Document document = new Document(finalValue);
            Metadata metadata = document.metadata();

            metadata.put("title", row.instruction());
            metadata.put("description", row.output());

            return splitter.split(document);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return List.of();
    }

    public List<TextSegment> splitDocument(Document document) {
        return splitter.split(document);
    }

    public List<String> chunkText(String text) {
        try {
            return semanticChunkingService.chunkText(text);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return List.of();
    }
}
    
