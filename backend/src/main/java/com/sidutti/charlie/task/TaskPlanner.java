package com.sidutti.charlie.task;

import com.sidutti.charlie.agent.AgentRegistry;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.FeatureMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TaskPlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPlanner.class);

    private final ChatModel chatModel;
    private final AgentRegistry agentRegistry;

    @Value("classpath:/prompts/task-planner-choose-agents.st")
    private Resource chooseAgentsUserPrompt;

    @Value("classpath:/prompts/task-planner-choose-agent.st")
    private Resource chooseAgentUserPrompt;

    public TaskPlanner(@Qualifier("vertexAiGeminiChat") ChatModel chatModel, AgentRegistry agentRegistry) {
        this.chatModel = chatModel;
        this.agentRegistry = agentRegistry;
    }

    public List<TaskAssignment> assign(List<Task> tasks) {
        return tasks.stream()
                .map(task -> new TaskAssignment(task, chooseAgent(chatModel, task).orElse(null)))
                .toList();
    }


    public Optional<String> chooseAgent(ChatModel chatModel, Task task) {
        long start = System.currentTimeMillis();
        Map<String, WorkerService> agents = agentRegistry.enabledAgents();

        LOGGER.info("Found {} enabled agents", agents.size());
//        agents.forEach((k, v) -> LOGGER.info("Worker: {}, Goal: {}", k, v.getGoal()));

        if (task.getAgent() != null) {
            LOGGER.info("Task specified agent: {}", task.getAgent());
            return agents.keySet().stream().filter(s -> s.equals(task.getAgent())).findFirst();
        }

        if (agents.isEmpty()) {
            LOGGER.warn("No agents available");
            return Optional.empty();
        }

        if (agents.size() == 1) {
            var agent = agents.entrySet().stream().findFirst().get();
            LOGGER.warn("Only one agent available: {}", agent);
            return Optional.ofNullable(agent.getKey());
        }

        StringBuilder agentList = new StringBuilder();
        for (Map.Entry<String, WorkerService> entry : agents.entrySet()) {
            agentList.append(entry.getKey()).append(": ").append(entry.getValue().getGoal()).append("\r\n");
        }

        PromptTemplate promptTemplate = new PromptTemplate(chooseAgentUserPrompt, Map.of(
                "task", task.getDescription(),
                "agents", agentList.toString()
        ));
        Prompt prompt = promptTemplate.create();

        var generation = chatModel.call(prompt).getResult();
        String content = generation.getOutput().getContent();
        Long elapsed = System.currentTimeMillis() - start;
        LOGGER.info("Selected Worker: {} in {} ms", content, elapsed);

        return Optional.ofNullable(content);
    }

    private Map<String, Object> chooseAgents(ChatModel chatModel, List<Task> tasks) {
        Map<String, WorkerService> agents = agentRegistry.enabledAgents();

        if (agents.isEmpty()) {
            LOGGER.warn("No agents available");
            return Map.of();
        }

        LOGGER.info("Found {} enabled agents", agents.size());
        agents.forEach((k, v) -> LOGGER.info("Worker: {}, Goal: {}", k, v.getGoal()));

        var outputConverter = new MapOutputConverter();

        StringBuilder agentList = new StringBuilder();
        for (Map.Entry<String, WorkerService> entry : agents.entrySet()) {
            var agentName = entry.getKey();
            var agent = entry.getValue();
            var agentGoal = agent.getGoal();
            var tools = agent.getTools();
            StringBuilder toolList = generateToolListForPrompt(tools);
            agentList.append(agentName).append(": ").append(agentGoal)
                    .append("\r\n")
                    .append(toolList);
        }

        StringBuilder taskList = new StringBuilder();
        for (Task task : tasks) {
            taskList.append(task.getDescription()).append("\r\n");
        }

        PromptTemplate promptTemplate = new PromptTemplate(chooseAgentsUserPrompt, Map.of(
                "tasks", taskList.toString(),
                "agents", agentList.toString(),
                "format", outputConverter.getFormat()
        ));
        Prompt prompt = promptTemplate.create();

        var generation = chatModel.call(prompt).getResult();
        String content = generation.getOutput().getContent();

        return outputConverter.convert(content);
    }

    private StringBuilder generateToolListForPrompt(Map<String, FeatureMetaData> tools) {
        StringBuilder toolList = new StringBuilder();
        toolList.append("The tools available from this agent are: ");
        for (Map.Entry<String, FeatureMetaData> toolEntry : tools.entrySet()) {
            var toolName = toolEntry.getKey();
            var toolDesc = toolEntry.getValue().description();
            boolean toolDisabled = toolEntry.getValue().disabled();
            if (!toolDisabled) {
                toolList.append("- ").append(toolName).append(": ").append(toolDesc).append("\r\n");
            }
        }
        return toolList;
    }
}
