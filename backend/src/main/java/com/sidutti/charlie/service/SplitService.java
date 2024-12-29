package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SplitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SplitService.class);
    TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
            .withChunkSize(600)
            .withMaxNumChunks(1000000)
            .withMinChunkLengthToEmbed(1)
            .build();

    public List<Document> splitDocument(Root.Row row) {
        try {
            String finalValue = row.instruction().concat(row.output());
            return tokenTextSplitter.split(Document.builder().text(finalValue).build());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return List.of();
    }

    public List<Document> splitDocument(Document inputText) {


        return tokenTextSplitter.split(inputText);

    }


}
    
