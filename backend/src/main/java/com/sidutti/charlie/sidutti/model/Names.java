package com.sidutti.charlie.sidutti.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public record Names(@Id ObjectId id, @Indexed String nconst,
                    String primaryName,
                    int birthYear,
                    int deathYear,
                    List<String> primaryProfession,
                    List<String> knownForTitles) {
}
