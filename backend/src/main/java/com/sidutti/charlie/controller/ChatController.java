package com.sidutti.charlie.controller;

import com.sidutti.charlie.model.ChatData;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@CrossOrigin(origins = "*")
@RestController
public class ChatController {
    private final ChatModel chatModel;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping(value = "/ai/chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<ChatData> chat(@RequestBody String question) {
        return chatModel.stream(question)
                .map(ChatData::new);
    }

}
