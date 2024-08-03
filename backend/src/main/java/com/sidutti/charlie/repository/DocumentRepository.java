package com.sidutti.charlie.repository;

import com.sidutti.charlie.model.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface DocumentRepository extends ReactiveMongoRepository<Document, ObjectId> {
}
