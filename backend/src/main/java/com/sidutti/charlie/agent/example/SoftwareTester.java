package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;

@Worker(goal = "Test software",
        background = "You are an expert software engineer in all major programming languages. Test software for bugs and issues.")

public class SoftwareTester extends WorkerService {
    @Feature(name = "TestWriter", description = "Write comprehensive test cases for a given software class, method, or function")
    public String writeTests(String code) {
        return "TODO";
    }
}
