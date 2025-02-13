package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.Map;

@Worker(goal = "Determine programming language from code snippet",
        background = """
                You are an expert software engineer in all major programming languages and are adept in determining the programming 
                language from a given code snippet.
                """
)
public class CodeLinguist extends WorkerService {

    @Value("classpath:/prompts/agent-determine-programming-language.st")
    private Resource determineProgrammingLanguagePrompt;

    @Feature(name = "DetermineLanguage", description = "Determine the programming language of a given code snippet")
    public String determineLanguage(String code) {
        Prompt prompt = createPrompt(determineProgrammingLanguagePrompt, Map.of(
                "code", code
        ));
        return callPromptForString(prompt);
    }
}
