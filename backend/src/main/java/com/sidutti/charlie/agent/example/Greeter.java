package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;

@Worker(goal = "Say hello", background = "You are a friendly person and greet everyone you encounter")
public class Greeter extends WorkerService {
    @Feature(name = "SayHello", description = "Be friendly and say hello")
    public String sayHello() {
        return "Hello, World!";
    }
}
