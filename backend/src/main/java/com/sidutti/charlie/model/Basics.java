package com.sidutti.charlie.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public record Basics(@Id ObjectId id,
                     @Indexed String tconst,
                     String titleType,
                     String primaryTitle,
                     String originalTitle,
                     String isAdult,
                     int startYear,
                     int endYear,
                     int runtimeMinutes,
                     List<String> genres
) {
}
