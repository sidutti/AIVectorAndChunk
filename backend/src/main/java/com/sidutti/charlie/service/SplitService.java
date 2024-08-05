package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Root;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class SplitService {
    private final DocumentBySentenceSplitter splitter;

    public SplitService() {
        this.splitter = new DocumentBySentenceSplitter(512,256);
    }
    public List<TextSegment> splitDocument(Root.Row row) {
        String finalValue = row.instruction().concat(row.output());
        Document document = new Document(finalValue);
        Metadata metadata = document.metadata();

        metadata.put("title", row.instruction());
        metadata.put("description", row.output());

        return splitter.split(document);
    }

}
    
