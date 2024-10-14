package com.sidutti.charlie.controller;

import com.sidutti.charlie.model.ChatData;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Stream;

@CrossOrigin(origins = "*")
@RestController
public class ChatController {
    private final ChatModel chatModel;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping(value = "/ai/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ChatData> chat(@RequestBody String question) {
        return chatModel.stream(question)
                .map(ChatData::new);
    }

    @PostMapping(value = "/ai/rag/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ChatData> rag(@RequestBody String question) {
        String[] inputs = question.split("###");
        String PROMPT_BLUEPRINT = """
                  Answer the query strictly referring the provided context:
                  {context}
                  Query:
                  {query}
                  In case you don't have any answer from the context provided, just say:
                  I'm sorry I don't have the information you are looking for.
                """;
        PromptTemplate promptTemplate = new PromptTemplate(PROMPT_BLUEPRINT);
        promptTemplate.add("query", inputs[0]);
        promptTemplate.add("context", inputs[1]);
        return chatModel.stream(promptTemplate.create())
                .map(ChatResponse::getResults)
                .map(this::createChatData)
                .flatMapIterable(Stream::toList);
    }

    private Stream<ChatData> createChatData(List<Generation> generations) {
        return generations.stream().map(generation -> new ChatData(generation.getOutput().getContent()));
    }

}
