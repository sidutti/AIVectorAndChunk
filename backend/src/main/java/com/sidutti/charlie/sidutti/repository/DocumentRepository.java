package com.sidutti.charlie.sidutti.repository;

import com.sidutti.charlie.sidutti.model.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface DocumentRepository extends ReactiveMongoRepository<Document, ObjectId> {
}
