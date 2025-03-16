package com.sidutti.charlie.model.repository;

import com.sidutti.charlie.model.neo.Chunk;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface ChunkRepository extends ReactiveNeo4jRepository<Chunk, String> {
}
