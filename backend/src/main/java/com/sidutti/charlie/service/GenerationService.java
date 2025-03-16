package com.sidutti.charlie.service;

import com.sidutti.charlie.model.PolicyData;
import com.sidutti.charlie.model.repository.PolicyDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.UUID;


@Component
public class GenerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationService.class);
    private final ChatModel chatModel;
    private final PolicyDataRepository repository;


    public GenerationService(@Qualifier("azureOpenAiChatModel") ChatModel chatModel,
                             PolicyDataRepository repository) {
        this.chatModel = chatModel;
        this.repository = repository;

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


}
