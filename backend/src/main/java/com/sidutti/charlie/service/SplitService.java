package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Root;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class SplitService {
    private final DocumentSplitter splitter;

    public SplitService() {
        this.splitter = DocumentSplitters.recursive(512, 256, new HuggingFaceTokenizer());
    }

    public List<TextSegment> splitDocument(Root.Row row) {
        String finalValue = row.instruction().concat(row.output());
        Document document = new Document(finalValue);
        Metadata metadata = document.metadata();

        metadata.put("title", row.instruction());
        metadata.put("description", row.output());

        return splitter.split(document);
    }

    public List<TextSegment> splitDocument(Document document) {
        return splitter.split(document);
    }
}
    
