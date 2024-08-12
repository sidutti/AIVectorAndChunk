package com.sidutti.charlie.controller;

import org.springframework.ai.chat.model.ChatModel;
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

    @PostMapping("/ai/chat")
    public Flux<String> chat(@RequestBody String question) {
        return chatModel.stream(question);
    }
}
