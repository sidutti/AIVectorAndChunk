package com.sidutti.charlie.task;

import com.sidutti.charlie.agent.AgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentTaskExecutor.class);


    private final ChatModel chatModel;
    private final TaskPlanner taskPlanner;
    private final AgentRegistry agentRegistry;

    public AgentTaskExecutor(@Qualifier("vertexAiGeminiChat") ChatModel chatModel,
                             TaskPlanner taskPlanner,
                             AgentRegistry agentRegistry) {
        this.chatModel = chatModel;
        this.taskPlanner = taskPlanner;
        this.agentRegistry = agentRegistry;
    }

    public TaskResult executeTask(Task task) throws IllegalStateException {
        LOGGER.info("Executing task: {}", task);
        return taskPlanner.chooseAgent(chatModel, task)
                .map(agentRegistry::getAgent)
                .map(agent -> agent.executeTask(task))
                .orElseThrow(() -> handleNoToolAvailable(task));
    }

    public List<TaskResult> executeTasks(ChatModel chatModel, List<Task> tasks) throws IllegalStateException {
        return tasks.stream()
                .map(task -> taskPlanner.chooseAgent(chatModel, task)
                        .map(agentRegistry::getAgent)
                        .map(agent -> agent.executeTask(task))
                        .orElseThrow(() -> handleNoToolAvailable(task))
                )
                .toList();
    }

    private IllegalStateException handleNoToolAvailable(Task task) {
        LOGGER.error("No tool available to execute task: {}", task);
        return new IllegalStateException("No tool available to execute task");
    }
}
