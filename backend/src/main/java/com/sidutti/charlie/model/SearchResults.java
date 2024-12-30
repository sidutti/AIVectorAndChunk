package com.sidutti.charlie.model;

public record SearchResults(String content,
                            String formatedContent,
                            String title,
                            String source,
                            String id,
                            float distance
) {
}
