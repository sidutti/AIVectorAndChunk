package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Component
public class SplitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SplitService.class);

    public List<String> splitDocument(Root.Row row) {
        try {
            String finalValue = row.instruction().concat(row.output());
            return splitStringToTokenLimit(finalValue, 2);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return List.of();
    }

    public List<String> splitDocument(Document inputText) {
        return splitStringToTokenLimit(inputText.getFormattedContent(), 3);
    }


    private List<String> splitStringToTokenLimit(String documentStr, int tokenLimit) {
        String[] sentences = documentStr.split("[.!?]");

        List<List<String>> groupedSentences = groupArray(sentences, tokenLimit);
        List<String> result = new ArrayList<>();
        for (List<String> group : groupedSentences) {
            result.add(String.join(" ", group));
        }
        return result;
    }

    public <T> List<List<T>> groupArray(T[] array, int groupSize) {
        List<List<T>> result = new ArrayList<>();
        int fromIndex = 0;

        while (fromIndex < array.length) {
            int toIndex = Math.min(fromIndex + groupSize, array.length);
            result.add(new ArrayList<>(Arrays.asList(array).subList(fromIndex, toIndex)));
            fromIndex = toIndex;
        }

        return result;
    }
}
    
