package com.sidutti.charlie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidutti.charlie.model.PolicyData;
import com.sidutti.charlie.model.neo.Chunk;
import com.sidutti.charlie.model.neo.Policy;
import com.sidutti.charlie.model.repository.ChunkRepository;
import com.sidutti.charlie.model.repository.PolicyDataRepository;
import com.sidutti.charlie.model.repository.PolicyRepository;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class GraphService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphService.class);

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an assistant that gives out Cypher code snippets.
            Use the information from the DOCUMENTS section only to provide accurate answers.
            Return just the code snippet without formatting. No descriptive text.
            Don't use any learned knowledge that is not within the DOCUMENTS section.
            
            DOCUMENTS:
            {documents}""";

    private final VectorStore store;
    private final PolicyRepository policyRepository;
    private final ChunkRepository chunkRepository;
    private final PolicyDataRepository repository;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;

    public GraphService(@Qualifier("azureOpenAiChatModel") ChatModel chatModel,

                        VectorStore store,
                        PolicyRepository policyRepository,
                        ChunkRepository chunkRepository,
                        PolicyDataRepository repository,
                        ObjectMapper objectMapper) {
        this.store = store;
        this.chatModel = chatModel;
        this.policyRepository = policyRepository;
        this.chunkRepository = chunkRepository;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Flux<Document> processRecords() {
        return repository
                .findAll()
                .map(this::cleanUpJson)
                .flatMap(this::savePolicy)
                .flatMapIterable(this::processRootPolicy)
                .flatMap(chunkRepository::save)
                .map(this::createChunk);

    }

    private Document createChunk(Chunk chunk) {
        Document doc = Document.builder()
                .id(chunk.chunkId())
                .text(chunk.chunkText())
                .metadata(Map.of("name", chunk.policyName()))
                .build();

        store.add(Collections.singletonList(doc));
        return doc;
    }


    private PolicyData cleanUpJson(PolicyData policyData) {
        String jsonContent = policyData.content().replaceAll("`", "");
        jsonContent = jsonContent.replaceAll("json", "").replaceAll("\n", "");
        return new PolicyData(policyData.id(), policyData.name(), jsonContent, policyData.entities(), policyData.date());
    }

    private Mono<Policy> savePolicy(PolicyData policyData) {
        Policy policy = new Policy(policyData.name(), policyData.id(), policyData.content());
        return policyRepository.save(policy);
    }

    private List<Chunk> processRootPolicy(Policy policy) {
        List<Chunk> chunks = new ArrayList<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(policy.policyText());
            Map<String, Object> nodes = new HashMap<>();
            extractNodes(jsonNode, nodes, null);
            for (Map.Entry<String, Object> entry : nodes.entrySet()) {
                String nodeKey = entry.getKey();
                Object nodeValue = entry.getValue();
                Chunk chunk = new Chunk(nodeKey,
                        nodeValue.toString(),
                        policy.policyName(),
                        policy);
                chunks.add(chunk);
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing JSON content for policy: {}", policy.policyId(), e);
        }
        return chunks;
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
                Analyze the following contract document and extract the entities in the following structured format. If the information is not explicitly stated in the document, indicate "Not Specified" or "N/A".
                
                     **Contract Entities:**
                
                     *   **Contract Name/Title:**
                     *   **Parties:**
                         *   **Party 1 Name:**
                         *   **Party 1 Address:**
                         *   **Party 1 Role:** (e.g., Seller, Buyer, Landlord, Tenant)
                         *   **Party 2 Name:**
                         *   **Party 2 Address:**
                         *   **Party 2 Role:**
                         *   **(Repeat for any additional parties)**
                     *   **Effective Date:** (YYYY-MM-DD)
                     *   **Term:**
                         *   **Start Date:** (YYYY-MM-DD)
                         *   **End Date:** (YYYY-MM-DD)
                         *   **Renewal Terms:** (Describe any automatic renewal clauses)
                     *   **Payment:**
                         *   **Currency:** (e.g., USD, EUR)
                         *   **Total Amount:**
                         *   **Payment Schedule:** (e.g., Monthly, Quarterly, Upon Completion)
                         *   **Payment Method:** (e.g., Wire Transfer, Check)
                     *   **Governing Law:**
                     *   **Termination Clause Summary:**
                     *   **Confidentiality Clause Summary:**
                     *   **Key Definitions:** (Extract and list any important defined terms in the contract)
                
                     Contract Document: %s
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
