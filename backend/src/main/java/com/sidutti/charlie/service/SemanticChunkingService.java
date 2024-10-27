package com.sidutti.charlie.service;


import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class SemanticChunkingService {


    private final EmbeddingModel embeddingModel;

    public SemanticChunkingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // Main function to chunk text into meaningful parts
    public List<String> chunkText(String text) {
        // Split text into individual sentences
        String[] singleSentencesList = splitSentences(text);
        // Combine adjacent sentences
        List<Map<String, String>> sentences = combineSentences(singleSentencesList);

        // Calculate cosine distances between combined sentences
        List<Double> distances = calculateCosineDistances(sentences);

        // Determine breakpoints for chunking
        double breakpointPercentileThreshold = 65;
        double breakpointDistanceThreshold = calculatePercentile(distances, breakpointPercentileThreshold);
        List<Integer> indicesAboveThresh = new ArrayList<>();
        for (int i = 0; i < distances.size(); i++) {
            if (distances.get(i) > breakpointDistanceThreshold) {
                indicesAboveThresh.add(i);
            }
        }

        // Create chunks based on breakpoints
        int startIndex = 0;
        List<String> chunks = new ArrayList<>();
        for (int index : indicesAboveThresh) {
            List<Map<String, String>> group = sentences.subList(startIndex, index + 1);
            StringBuilder combinedText = new StringBuilder();
            for (Map<String, String> sentence : group) {
                combinedText.append(sentence.get("sentence")).append(" ");
            }
            chunks.add(combinedText.toString().trim());
            startIndex = index + 1;
        }

        // Handle the last group, if any sentences remain
        if (startIndex < sentences.size()) {
            StringBuilder combinedText = new StringBuilder();
            for (int i = startIndex; i < sentences.size(); i++) {
                combinedText.append(sentences.get(i).get("sentence")).append(" ");
            }
            chunks.add(combinedText.toString().trim());
        }

        return chunks;
    }

    // Function to split text into sentences
    private String[] splitSentences(String text) {
        return Pattern.compile("[.?!]").split(text);
    }

    // Function to combine adjacent sentences with a buffer
    private List<Map<String, String>> combineSentences(String[] sentences) {
        List<Map<String, String>> combinedSentences = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            StringBuilder combinedSentence = new StringBuilder();
            for (int j = i - 1; j < i; j++) {
                if (j >= 0) {
                    combinedSentence.append(sentences[j]).append(" ");
                }
            }
            combinedSentence.append(sentences[i]);
            for (int j = i + 1; j < i + 1 + 1; j++) {
                if (j < sentences.length) {
                    combinedSentence.append(" ").append(sentences[j]);
                }
            }
            Map<String, String> sentenceMap = new HashMap<>();
            sentenceMap.put("sentence", sentences[i]);
            sentenceMap.put("combined_sentence", combinedSentence.toString());
            combinedSentences.add(sentenceMap);
        }
        return combinedSentences;
    }

    // Function to calculate cosine distances between combined sentences
    private List<Double> calculateCosineDistances(List<Map<String, String>> sentences) {
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < sentences.size() - 1; i++) {
            String currentSentence = sentences.get(i).get("combined_sentence");
            String nextSentence = sentences.get(i + 1).get("combined_sentence");
            float[] currentEmbeddings = embeddingModel.embed(currentSentence);
            float[] nextEmbeddings = embeddingModel.embed(nextSentence);

            double distance = 1;
            distances.add(distance);
            sentences.get(i).put("distance_to_next", String.valueOf(distance));
        }
        return distances;
    }


    // Helper function to calculate percentile
    private double calculatePercentile(List<Double> distances, double percentile) {
        int size = distances.size();
        int index = (int) (size * percentile / 100);
        Collections.sort(distances);
        return distances.get(index);
    }


}
