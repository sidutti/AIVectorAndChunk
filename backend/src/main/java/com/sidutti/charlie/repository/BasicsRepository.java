package com.sidutti.charlie.repository;

import com.sidutti.charlie.model.Basics;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BasicsRepository extends ReactiveMongoRepository<Basics, ObjectId> {
}
