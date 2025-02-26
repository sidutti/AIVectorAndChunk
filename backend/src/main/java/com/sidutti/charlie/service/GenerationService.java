package com.sidutti.charlie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidutti.charlie.model.PolicyData;
import com.sidutti.charlie.model.repository.PolicyDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class GenerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationService.class);
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final PolicyDataRepository repository;
    private final Neo4jVectorStore store;


    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an assistant that gives out Cypher code snippets.
            Use the information from the DOCUMENTS section only to provide accurate answers.
            Return just the code snippet without formatting. No descriptive text.
            Don't use any learned knowledge that is not within the DOCUMENTS section.
            
            DOCUMENTS:
            {documents}""";

    public GenerationService(@Qualifier("azureOpenAiChatModel") ChatModel chatModel,
                             ObjectMapper objectMapper,
                             PolicyDataRepository repository,
                             Neo4jVectorStore store) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.store = store;

    }

    public void processFiles() {
        try (InputStream resourceAsStream = new ClassPathResource("GeneralPolicies").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] textContext = line.split("###&&&###");

                String prompt = textContext[0];
                String context = textContext[1];

                generateGeneralPolicy(prompt, context)
                        .reduce((a, b) -> a + b)
                        .map(s -> createPolicyData(s, prompt))
                        .flatMap(repository::save)
                        .subscribe();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        try (InputStream resourceAsStream = new ClassPathResource("PolicyList").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String line;
            while ((line = reader.readLine()) != null) {

                String finalLine = line;
                generateBankPolicy(line)
                        .reduce((a, b) -> a + b)
                        .map(s -> createPolicyData(s, finalLine))
                        .flatMap(repository::save)
                        .subscribe();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    public void processRecords() {
        repository
                .findAll()
                .map(this::processPolicyData)
                .subscribe();
    }

    private PolicyData createPolicyData(String content, String prompt) {
        return new PolicyData(UUID.randomUUID().toString(),
                prompt,
                content,
                null,
                new Date());
    }

    public Flux<String> generateGeneralPolicy(String prompt, String context) {

        String userText = """
                You are a Policy writer for a large bank. The name of the bank is Acme.
                Break the policy into sections if needed.
                Return just the Policy as JSON. No descriptive text.
                Generate policy for the %s. Use the context to generate
                %s
                """;
        var userMessage = new UserMessage(userText.formatted(prompt, context));
        return chatModel.stream(userMessage);
    }

    public Flux<String> generateBankPolicy(String prompt) {

        String userText = """
                You are a Policy writer for a large bank. Write the policy for US.  The name of the bank is Acme.
                Break the policy into sections if needed.
                Return just the Policy as JSON. No descriptive text.
                Generate policy for the %s.
                """;
        var userMessage = new UserMessage(userText.formatted(prompt));
        return chatModel.stream(userMessage);
    }

    private String processPolicyData(PolicyData policyData) {
        String jsonContent = policyData.content().replaceAll("`","");
        jsonContent = jsonContent.replaceAll("json", "").replaceAll("\n","");
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            Map<String, Object> nodes = new HashMap<>();
            extractNodes(jsonNode, nodes, null);
            for (Map.Entry<String, Object> entry : nodes.entrySet()) {
                String nodeKey = entry.getKey();
                Object nodeValue = entry.getValue();
                Document doc = Document.builder()
                        .id(nodeKey)
                        .text(nodeValue.toString())
                        .build();
                store.add(Collections.singletonList(doc));
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing JSON content for policy: {}", policyData.id(), e);
        }
        return jsonContent;
    }

    private void extractNodes(JsonNode node, Map<String, Object> nodes, String parentKey) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String compositeKey = parentKey == null ? key : parentKey + "." + key;
                extractNodes(entry.getValue(), nodes, compositeKey);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String arrayKey = parentKey + "[" + i + "]";
                extractNodes(node.get(i), nodes, arrayKey);
            }
        } else if (node.isValueNode()) {
            nodes.put(parentKey, node.asText());
        }
    }

    public Flux<String> extractEntities(String prompt) {

        String userText = """
                You are a Knowledge Graph expert, extract the following entities from the text.
                Entities: Employees, supervisors, managers,clients, Customers, data processors, third-party vendors ,System administrators, network users, external access providers
                Text: %s
                """;
        var userMessage = new UserMessage(userText.formatted(prompt));
        return chatModel.stream(userMessage);
    }

    public Flux<String> convertToCypher(String message) {

        var systemMessage = new SystemPromptTemplate(SYSTEM_PROMPT_TEMPLATE)
                .createMessage(Map.of("documents", message));

        var userMessage = new UserMessage(message);

        var prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatModel.stream(prompt)
                .flatMapIterable(ChatResponse::getResults)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText);
    }
}
