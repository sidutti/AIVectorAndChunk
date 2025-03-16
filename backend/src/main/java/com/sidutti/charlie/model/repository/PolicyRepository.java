package com.sidutti.charlie.model.repository;

import com.sidutti.charlie.model.neo.Policy;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface PolicyRepository extends ReactiveNeo4jRepository<Policy, String> {
}
