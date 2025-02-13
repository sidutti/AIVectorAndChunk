package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.agent.example.model.JobRateRequest;
import com.sidutti.charlie.agent.example.model.JobRateResponse;
import com.sidutti.charlie.tool.Feature;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Worker(goal = "Provide guidance and support to an individual in their search for a job")
@RestController
@RequestMapping("/api/agents/job-recruiter")
public class JobRecruiter extends WorkerService {

    @Value("classpath:/prompts/agent-job-rating.st")
    private Resource jobRaterPrompt;

    @PostMapping("/rate-job")
    @Feature(name = "JobRater", description = "Given a job description, rate the job based on how well it matches the user's skills and interests")
    public JobRateResponse rateJob(@RequestBody JobRateRequest jobRateRequest) {
        var outputConverter = new BeanOutputConverter<>(JobRateResponse.class);

        PromptTemplate promptTemplate = new PromptTemplate(jobRaterPrompt,
                Map.of(
                        "jobDescription", jobRateRequest.jobDescription(),
                        "qualifications", jobRateRequest.qualificationsForPrompt(),
                        "interests", jobRateRequest.interestsForPrompt(),
                        "format", outputConverter.getFormat()
                )
        );
        Prompt prompt = promptTemplate.create();

        Generation generation = chatModel.call(prompt).getResult();

        return outputConverter.convert(generation.getOutput().getContent());
    }
}
