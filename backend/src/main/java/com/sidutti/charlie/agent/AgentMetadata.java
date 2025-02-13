package com.sidutti.charlie.agent;



import com.sidutti.charlie.tool.FeatureMetaData;

import java.util.Map;

public record AgentMetadata(String name, String goal, String background, Map<String, FeatureMetaData> tools) {
}
