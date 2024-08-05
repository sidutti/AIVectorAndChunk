package com.sidutti.charlie.model;

import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;

@org.springframework.data.elasticsearch.annotations.Document(createIndex = false, indexName = "spring-ai-document-index")
public record Document(@Id String id,
                       Map<String, Object> metadata,
                       String content,
                       List<Double> embedding) {

}
