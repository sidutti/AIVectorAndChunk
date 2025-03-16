package com.sidutti.charlie.model.neo;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("chunk")
public record Chunk(@Id String chunkId,
                    String chunkText,
                    String policyName,
                    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING) Policy policy) {
}
