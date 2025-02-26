package com.sidutti.charlie.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;

@Document(indexName = "policy-data")
public record PolicyData(@Id String id,
                         String name,
                         String content,
                         String entities,
                         @Field(type = Date) java.util.Date date) {
}
