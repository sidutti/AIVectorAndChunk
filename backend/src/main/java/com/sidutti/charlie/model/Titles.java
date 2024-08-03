package com.sidutti.charlie.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record Titles(@Id ObjectId id,
                     @Indexed String titleId,
                     int ordering,
                     String title,
                     String region,
                     String language,
                     String types,
                     String attributes,
                     int isOriginalTitle) {
}
