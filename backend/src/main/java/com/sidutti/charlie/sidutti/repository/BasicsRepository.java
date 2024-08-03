package com.sidutti.charlie.sidutti.repository;

import com.sidutti.charlie.sidutti.model.Basics;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BasicsRepository extends ReactiveMongoRepository<Basics, ObjectId> {
}
