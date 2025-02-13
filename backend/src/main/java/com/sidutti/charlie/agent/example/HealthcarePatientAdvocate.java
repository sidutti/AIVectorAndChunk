package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Worker(goal = "Provide assistance to patients with healthcare needs")
@Component
@Profile("healthcare")
public class HealthcarePatientAdvocate extends WorkerService {

    private final VectorStore vectorStore;
    private final ChatModel model;

    public HealthcarePatientAdvocate(VectorStore vectorStore, @Qualifier("vertexAiGeminiChat") ChatModel model) {
        this.vectorStore = vectorStore;
        this.model = model;
    }

    @Feature(name = "Healthcare benefits query interface", description = "Provide information about healthcare insurance benefits")
    public String getHealthcareBenefitsInfo(String healthcareBenefitsQuestion) {

        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest
                        .builder()
                        .query(healthcareBenefitsQuestion)
                        .topK(1)
                        .build()
        );
        assert similarDocuments != null;
        String content = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        var systemPromptTemplate = """
                You are a helpful assistant, conversing with a user about health benefits available to them through Providence HealthPlan insurance.
                Use the information from the DOCUMENTS section to provide accurate answers. If unsure or if the answer
                isn't found in the DOCUMENTS section, simply state that you don't know the answer and do not mention
                the DOCUMENTS section.
                
                ## DOCUMENTS:
                
                {documents}
                """;

        return ChatClient.create(model)
                .prompt()
                .system(sysSpec -> sysSpec.text(systemPromptTemplate).param("documents", content))
                .user(healthcareBenefitsQuestion)
                .call()
                .content();
    }
}
