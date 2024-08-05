package com.sidutti.charlie.model;

public record SearchResults(String content, String formatedContent,
                            String id,
                            float distance
) {
}
