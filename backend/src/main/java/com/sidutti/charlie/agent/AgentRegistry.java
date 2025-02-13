package com.sidutti.charlie.agent;

import com.sidutti.charlie.tool.Feature;

import com.sidutti.charlie.tool.FeatureMetaData;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AgentRegistry {

    private final Map<String, WorkerService> allAgents = new HashMap<>();
    private final ApplicationContext applicationContext;

    public AgentRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initializeAgents() {
        Map<String, WorkerService> agentBeans = applicationContext.getBeansOfType(WorkerService.class);
        agentBeans.values().forEach(agent -> {
            addTools(agent);
            registerAgent(agent);
        });
    }

    private void addTools(WorkerService agent) {
        Arrays.stream(agent.getClass().getDeclaredMethods())
                .filter(this::hasToolAnnotation)
                .map(this::createTool)
                .forEach(agent::addTool);
    }

    private boolean hasToolAnnotation(Method method) {
        return AnnotationUtils.findAnnotation(method, Feature.class) != null;
    }

    private FeatureMetaData createTool(Method method) {
        Feature feature = AnnotationUtils.findAnnotation(method, Feature.class);
        String name = Objects.requireNonNull(feature).name() != null ? feature.name() : "";
        String description = feature.description() != null ? feature.description() : "";
        boolean disabled = feature.disabled();
        return new FeatureMetaData(name, description, method, disabled);
    }

    private void registerAgent(WorkerService agent) {
        allAgents.put(agent.getName(), agent);
    }

    public WorkerService getAgent(String agentName) {
        return allAgents.get(agentName);
    }

    public Map<String, WorkerService> allAgents() {
        return allAgents;
    }

    public Map<String, WorkerService> enabledAgents() {
        return allAgents.entrySet().stream()
                .filter(entry -> !entry.getValue().getDisabled())
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), Map::putAll);
    }
}
