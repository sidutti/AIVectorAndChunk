package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


@Component
public class SplitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SplitService.class);

    private final SemanticChunkingService semanticChunkingService;

    public SplitService(SemanticChunkingService semanticChunkingService) {
        this.semanticChunkingService = semanticChunkingService;
    }

    public List<String> splitDocument(Root.Row row) {
        try {
            String finalValue = row.instruction().concat(row.output());
            return splitStringToTokenLimit(finalValue, 500);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return List.of();
    }

    public List<String> splitDocument(String inputText) {
        return splitStringToTokenLimit(inputText, 384);
    }

    public List<String> splitDocument(Document inputText) {
        return splitStringToTokenLimit(inputText.getFormattedContent(), 384);
    }

    public List<String> chunkText(String text) {
        try {
            return semanticChunkingService.chunkText(text);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return List.of();
    }

    private List<String> splitStringToTokenLimit(String documentStr, int tokenLimit) {
        List<String> splitStrings = new ArrayList<>();
        var tokens = new StringTokenizer(documentStr).countTokens();
        var chunks = Math.ceilDiv(tokens, tokenLimit);
        if (chunks == 0) {
            return splitStrings;
        }
        var chunkSize = Math.ceilDiv(documentStr.length(), chunks);

        while (!documentStr.isBlank()) {
            splitStrings
                    .add(documentStr.length() > chunkSize ? documentStr.substring(0, chunkSize) : documentStr);
            documentStr = documentStr.length() > chunkSize ? documentStr.substring(chunkSize) : "";
        }
        return splitStrings;
    }

    private String cutStringToTokenLimit(String cutString, int tokenLimit) {
        while (tokenLimit < new StringTokenizer(cutString, " -.;,").countTokens()) {
            cutString = cutString.length() > 100 ? cutString.substring(0, cutString.length() - 100) : "";
        }
        return cutString;
    }
}
    
