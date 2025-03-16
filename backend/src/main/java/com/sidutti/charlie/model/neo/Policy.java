package com.sidutti.charlie.model.neo;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("policy")
public record Policy(@Id String policyName,
                     String policyId,
                     String policyText) {
}
