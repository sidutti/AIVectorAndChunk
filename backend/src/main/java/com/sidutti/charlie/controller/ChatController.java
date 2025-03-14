package com.sidutti.charlie.controller;

import com.sidutti.charlie.cloud.google.BatchDocumentService;
import com.sidutti.charlie.cloud.google.DocumentService;
import com.sidutti.charlie.model.ChatData;
import com.sidutti.charlie.model.ExtractedDocument;
import com.sidutti.charlie.service.GenerationService;
import com.sidutti.charlie.tool.TransformerUtil;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@Configuration
public class ChatController {
    private final ChatModel chatModel;
    private final DocumentService service;

    private final TransformerUtil util;
    private final BatchDocumentService batchDocumentService;
    private final GenerationService generationService;

    public ChatController(@Qualifier("azureOpenAiChatModel") ChatModel chatModel,
                          DocumentService service,
                          TransformerUtil util,
                          BatchDocumentService batchDocumentService,
                          GenerationService generationService) {
        this.chatModel = chatModel;
        this.service = service;
        this.util = util;
        this.batchDocumentService = batchDocumentService;
        this.generationService = generationService;
    }

    @Bean
    public RouterFunction<ServerResponse> chattingRoutes() {
        return route(POST("ai/chat"), this::chat)
                .andRoute(POST("ai/plainchat"), this::plainChat)
                .andRoute(POST("ai/generatePolicy"), this::generatePolicy)
                .andRoute(POST("ai/processRecords"), this::processRecords)

                .andRoute(POST("ai/summarize"), this::summarize)
                .andRoute(POST("/ai/extract"), this::extract)
                .andRoute(POST("/ai/extract/batch"), this::batchExtract)
                .andRoute(POST("/ai/rag/generate"), this::rag);
    }

    private Mono<ServerResponse> generatePolicy(ServerRequest request) {
        generationService.processFiles();
        return ServerResponse.ok().bodyValue("Success");
    }
    private Mono<ServerResponse> processRecords(ServerRequest request) {
        generationService.processRecords();
        return ServerResponse.ok().bodyValue("Success");
    }

    public Mono<ServerResponse> plainChat(ServerRequest request) {
        Flux<String> result = request.bodyToMono(String.class)
                .flatMapMany(chatModel::stream);

        return ServerResponse.ok().body(result, String.class);
    }

    public Mono<ServerResponse> chat(ServerRequest request) {
        Flux<ChatData> result = request.bodyToMono(String.class)
                .flatMapMany(chatModel::stream)
                .map(ChatData::new);
        return ServerResponse.ok().body(result, ChatData.class);
    }

    public Mono<ServerResponse> summarize(ServerRequest request) {
        var mime = request.queryParam("mime").orElse("application/pdf");
        Flux<ChatData> result = request.body(BodyExtractors.toDataBuffers())
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)))
                .map(InputStreamResource::new)
                .map(is -> new UserMessage("Based on the provided document tell me what it is and your confidence level",
                        List.of(new Media(MimeType.valueOf(mime), is))))
                .flatMapMany(chatModel::stream)
                .map(ChatData::new);

        return ServerResponse.ok().body(result, ChatData.class);
    }

    private Mono<ServerResponse> batchExtract(ServerRequest request) {
        var mime = request.queryParam("mime").orElse("application/pdf");
        var fileName = request.queryParam("fileName").orElse("file");
        var parser = request.queryParam("parser").orElse("layout");
        var uuid = UUID.randomUUID().toString();
        Flux<ExtractedDocument> result = request.body(BodyExtractors.toDataBuffers())
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)))
                .map(is -> batchDocumentService.uploadFileToGCS(is, fileName))
                .flatMap(file -> batchDocumentService.processFile(mime, file, parser, uuid))
                .map(res -> batchDocumentService.processOutput(uuid, fileName))
                .flatMapMany(Flux::fromIterable)
                .map(util::transform);
        return ServerResponse.ok().body(result, ExtractedDocument.class);
    }

    public Mono<ServerResponse> extract(ServerRequest request) {
        var mime = request.queryParam("mime").orElse("application/pdf");
        var result = request.body(BodyExtractors.toDataBuffers())
                .reduce(InputStream.nullInputStream(), (s, d)
                        -> new SequenceInputStream(s, d.asInputStream(true)))
                .map(is -> service.processDocument(is, mime))
                .map(util::transform);

        return ServerResponse.ok().body(result, ExtractedDocument.class);
    }

    public Mono<ServerResponse> rag(ServerRequest request) {
        Flux<Generation> result = request.bodyToMono(String.class)
                .map(s -> s.split("###"))
                .map(this::getPrompt)
                .flatMapMany(chatModel::stream)
                .flatMapIterable(ChatResponse::getResults);

        return ServerResponse.ok().body(result, Generation.class);
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
