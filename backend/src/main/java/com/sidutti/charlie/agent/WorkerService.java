package com.sidutti.charlie.agent;


import com.sidutti.charlie.task.Task;
import com.sidutti.charlie.task.TaskResult;
import com.sidutti.charlie.task.TaskTiming;
import com.sidutti.charlie.tool.FeatureMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerService.class);

    @Qualifier("vertexAiGeminiChat")
    @Autowired
    protected ChatModel chatModel;

    @Value("classpath:/prompts/choose-tool.st")
    private Resource chooseToolUserPrompt;

    @Value("classpath:/prompts/choose-tool-args.st")
    private Resource chooseToolArgsUserPrompt;

    @Value("classpath:/prompts/choose-tool-args-no-format.st")
    private Resource chooseToolArgsUserPromptNoFormat;




    private final String name;

    private final String goal;

    private final String background;

    private final Boolean disabled;

    private final Map<String, FeatureMetaData> tools = new HashMap<>();

    List<Message> messages = new ArrayList<>();



    public Prompt createPrompt(Resource promptTemplateResource,
                               Map<String, Object> promptModel) {
        PromptTemplate promptTemplate = new PromptTemplate(promptTemplateResource, promptModel);
        return promptTemplate.create();
    }

    public String callPromptForString(Prompt prompt) {
        Generation generation = chatModel.call(prompt).getResult();
        return generation.getOutput().getContent();
    }

    public Object callPromptForBean(Prompt prompt, BeanOutputConverter<Object> beanOutputConverter) {
        Generation generation = chatModel.call(prompt).getResult();
        String out = generation.getOutput().getContent();
        return beanOutputConverter.convert(out);
    }

    public void addSystemMessage(String message) {
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(message);
        messages.add(systemTemplate.createMessage());
    }

    public void addUserMessage(String message) {
        UserMessage userMessage = new UserMessage(message);
        messages.add(userMessage);
    }

    public WorkerService() {
        this.name = this.getClass().getSimpleName();
        if (this.getClass().isAnnotationPresent(Worker.class)) {
            Worker annotation = this.getClass().getAnnotation(Worker.class);
            this.goal = annotation.goal();
            this.background = annotation.background();
            this.disabled = annotation.disabled();
        } else {
            throw new IllegalStateException("Worker annotation is required on Worker classes");
        }
    }

    public void addTool(FeatureMetaData featureMetaData) {
        tools.put(featureMetaData.name(), featureMetaData);
    }

    public TaskResult executeTask(Task task) {
        LOGGER.info("Executing task: {}", task);
        List<TaskTiming> timings = new ArrayList<>();

        if (tools.isEmpty()) {
            LOGGER.info("{} agent has no tools configured, executing task via LLM", this.name);
            return executeTaskViaLLM(task, timings);
        }

        var startChooseTool = System.currentTimeMillis();
        FeatureMetaData featureMetaData = chooseTool(task);
        timings.add(new TaskTiming("chooseTool", System.currentTimeMillis() - startChooseTool));
        if (featureMetaData.name().equals("__NO_TOOL__")) {
            return executeTaskViaLLM(task, timings);
        }

        var startGetArgs = System.currentTimeMillis();
        Object args = null;
        Class<?> returnType = featureMetaData.getReturnType();
        if (returnType != null) {
            if (returnType.isPrimitive() || "java.lang.String".equals(returnType.getName())) {
                args = getArgsAsString(task, featureMetaData);
            } else {
                args = getArgsAsObject(task, returnType, featureMetaData);
            }
        }
        timings.add(new TaskTiming("getArgs", System.currentTimeMillis() - startGetArgs));

        try {
            var startInvokeTool = System.currentTimeMillis();
            Object toolResult = invokeTool(featureMetaData.method(), args);
            timings.add(new TaskTiming("invokeTool", System.currentTimeMillis() - startInvokeTool));
            TaskResult tr = new TaskResult(task, this.name, featureMetaData.name(), toolResult, timings);
            LOGGER.info("TaskResult: {}", tr);
            return tr;
        } catch (Exception e) {
            throw new IllegalStateException("Error invoking tool: " + featureMetaData + " for task: " + task, e);
        }
    }

    private Object getArgsAsString(Task task, FeatureMetaData featureMetaData) {
        Prompt prompt = createPrompt(chooseToolArgsUserPromptNoFormat, Map.of(
                "task", task.getDescription(),
                "signature", featureMetaData.getMethodArgsAsString()
        ));
        return callPromptForString(prompt);
    }

    private Object getArgsAsObject(Task task, Class<?> returnType, FeatureMetaData featureMetaData) {
        BeanOutputConverter<Object> outputConverter = new BeanOutputConverter(returnType);
        Prompt prompt = createPrompt(chooseToolArgsUserPrompt, Map.of(
                "task", task.getDescription(),
                "signature", featureMetaData.getMethodArgsAsString(),
                "format", outputConverter.getFormat()
        ));
        return callPromptForBean(prompt, outputConverter);
    }


    private <T> T invokeTool(Method method, Object args) throws Exception {
        if (args == null) {
            LOGGER.info("Invoking method: {}", method.toString());
            T result = (T) method.invoke(this);
            return result;
        } else {
            LOGGER.info("Invoking method: {} with args: {}", method.toString(), args);
            LOGGER.info("args type: {}", args.getClass().getName());
            T result = (T) method.invoke(this, args);
            return result;
        }
    }

    public void addDateContext() {
        String dateContext = "The date and time right now is: " +
                ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)) +
                ". Use this date to answer any questions related to the current date and time.";
        addSystemMessage(dateContext);
    }

    public void addMathInstructions() {
        addSystemMessage("If you perform any math calculations, please return the resulting number and nothing else. Do not return a sentence or any other text.");
    }

    protected TaskResult executeTaskViaLLM(Task task, List<TaskTiming> timings) {

        var startLLM = System.currentTimeMillis();
        if (this.background != null && !this.background.isEmpty()) {
            addSystemMessage(this.background);
        }

        addDateContext();
        addMathInstructions();

        var taskDescription = task.getDescription();
        addUserMessage(taskDescription);

        Prompt prompt = new Prompt(messages);
        String data = callPromptForString(prompt);

        timings.add(new TaskTiming("executeViaLLM", System.currentTimeMillis() - startLLM));

        return new TaskResult(task, this.name, null, data, timings);
    }

    private FeatureMetaData chooseTool(Task task) {
        StringBuilder toolList = new StringBuilder();
        tools.values().stream()
                .map(featureMetaData -> featureMetaData.name() + ": " + featureMetaData.description() + "\r\n")
                .forEach(toolList::append);
        Prompt prompt = createPrompt(chooseToolUserPrompt, Map.of(
                "task", task.getDescription(),
                "tools", toolList.toString()
        ));
        String toolName = callPromptForString(prompt);
        if (toolName.equals("__NO_TOOL__")) {
            LOGGER.warn("No suitable tool found for task: {}", task.getDescription());
            return new FeatureMetaData("__NO_TOOL__", null, null, false);
        }
        FeatureMetaData tool = tools.get(toolName);
        LOGGER.info("Chosen tool: {}", tool);
        if (tool == null) {
            throw new IllegalStateException("No tool found with name: " + toolName);
        }
        return tool;
    }
    public String getName() {
        return name;
    }

    public String getGoal() {
        return goal;
    }

    public String getBackground() {
        return background;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public Map<String, FeatureMetaData> getTools() {
        return tools;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
