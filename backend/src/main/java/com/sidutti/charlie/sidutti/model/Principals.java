package com.sidutti.charlie.sidutti.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record Principals(@Id ObjectId id, @Indexed String tconst, int ordering, @Indexed String nconst, String category,
                         String job, String characters) {
}
