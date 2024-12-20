package com.sidutti.charlie.controller;

import com.sidutti.charlie.cloud.google.DocumentService;
import com.sidutti.charlie.model.ChatData;
import com.sidutti.charlie.model.ExtractedDocument;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@Configuration
public class ChatController {
    private final ChatModel chatModel;
    private final DocumentService service;
    private final WebClient client;

    public ChatController(VertexAiGeminiChatModel chatModel, DocumentService service, WebClient client) {
        this.chatModel = chatModel;
        this.service = service;
        this.client = client;
    }

    @Bean
    public RouterFunction<ServerResponse> chattingRoutes() {
        return route(POST("ai/chat"), this::chat)
                .andRoute(POST("ai/summarize"), this::summarize)
                .andRoute(POST("/ai/extract"), this::extract)
                .andRoute(POST("/ai/rag/generate"), this::rag)
                .andRoute(POST("/ai/extract/irs"), this::extractIrs);
    }

    public Mono<ServerResponse> chat(ServerRequest request) {
        Flux<ChatData> result = request.bodyToMono(String.class)
                .map(chatModel::stream)
                .flatMapMany(t -> t)
                .map(ChatData::new);
        return ServerResponse.ok().body(result, ChatData.class);
    }

    public Mono<ServerResponse> summarize(ServerRequest request) {
        Flux<ChatData> result = request.body(BodyExtractors.toDataBuffers())
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)))
                .map(InputStreamResource::new)
                .map(is -> new UserMessage("Based on the provided document tell me what it is and your confidence level",
                        List.of(new Media(MimeType.valueOf("application/pdf"), is))))
                .map(chatModel::stream)
                .flatMapMany(t -> t)
                .map(ChatData::new);

        return ServerResponse.ok().body(result, ChatData.class);
    }

    public Mono<ServerResponse> extract(ServerRequest request) {
        Mono<ExtractedDocument> result = request.body(BodyExtractors.toDataBuffers())
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)))
                .map(service::processDocument);

        return ServerResponse.ok().body(result, ChatData.class);
    }

    public Mono<ServerResponse> rag(ServerRequest request) {
        Flux<ChatData> result = request.bodyToMono(String.class)
                .map(s -> s.split("###"))
                .map(this::getPrompt)
                .map(chatModel::stream)
                .flatMapMany(t -> t)
                .map(ChatResponse::getResults)
                .flatMapIterable(l -> l)
                .map(g -> new ChatData(g.getOutput().getContent()));

        return ServerResponse.ok().body(result, ChatData.class);
    }

    public Mono<ServerResponse> extractIrs(ServerRequest request) {
        Mono<ExtractedDocument> result = request.bodyToMono(String.class)
                .flatMap(this::extractDocFromIRS)
                .map(service::processDocument);
        return ServerResponse.ok().body(result, ExtractedDocument.class);

    }

    private Mono<InputStream> extractDocFromIRS(String url) {
        return client
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .retryWhen(RetryBackoffSpec.backoff(3, java.time.Duration.ofSeconds(10)))
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)));
    }

    private Prompt getPrompt(String[] inputs) {
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
        return promptTemplate.create();
    }
}
