package com.sidutti.charlie.sidutti.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record Ratings(@Id ObjectId id, @Indexed String tconst, float averageRating, int numVotes) {
}
