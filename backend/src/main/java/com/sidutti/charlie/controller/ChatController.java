package com.sidutti.charlie.controller;

import com.sidutti.charlie.cloud.google.DocumentService;
import com.sidutti.charlie.model.ChatData;
import com.sidutti.charlie.model.ExtractedDocument;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Stream;

@CrossOrigin(origins = "*")
@RestController
public class ChatController {
    private final ChatModel chatModel;
    private final DocumentService service;
    private final WebClient client;

    public ChatController(VertexAiGeminiChatModel chatModel, DocumentService service, WebClient client) {
        this.chatModel = chatModel;
        this.service = service;
        this.client = client;
    }

    @PostMapping(value = "/ai/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ChatData> chat(@RequestBody String question) {
        return chatModel.stream(question)
                .map(ChatData::new);
    }


    @GetMapping(value = "/ai/summarize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> summarize(@RequestParam(value = "fileName") String fileName) throws MalformedURLException {
        var userMessage = new UserMessage("Based on the provided document tell me what it is and your confidence level",
                List.of(new Media(MimeType.valueOf("application/pdf"), new FileSystemResource(fileName))));

        return chatModel.stream(userMessage);
    }

    @GetMapping(value = "/ai/extract", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExtractedDocument extract(@RequestParam(value = "fileName") String fileName) throws IOException {
        return service.processDocument(fileName);
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

    @GetMapping(value = "/ai/extract/irs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ExtractedDocument> extractIrs(@RequestBody String url) throws IOException {
        return client
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .retryWhen(RetryBackoffSpec.backoff(3, java.time.Duration.ofSeconds(10)))
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)))
                .map(service::processDocument);

    }
}
