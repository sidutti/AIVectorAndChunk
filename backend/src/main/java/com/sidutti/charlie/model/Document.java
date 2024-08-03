package com.sidutti.charlie.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;

@org.springframework.data.mongodb.core.mapping.Document
public record Document(@Id ObjectId id,
                       Map<String, Object> metadata,
                       String content,
                       List<Double> embedding) {

}
