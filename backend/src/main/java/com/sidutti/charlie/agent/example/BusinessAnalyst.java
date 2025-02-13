package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.agent.example.model.CompanyDetail;
import com.sidutti.charlie.tool.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.Map;

@Worker(
        goal = "Provide business analysis of companies")
public class BusinessAnalyst extends WorkerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessAnalyst.class);
    @Value("classpath:/prompts/agent-company-focus.st")
    private Resource companyFocusUserPrompt;

    @Feature(name = "GetCompanyFocus", description = "Given the name of a company, return the focus of the company")
    public String getCompanyFocus(String companyName) {
        Prompt prompt = createPrompt(companyFocusUserPrompt, Map.of(
                "companyName", companyName
        ));
        return callPromptForString(prompt);
    }

    @Feature(name = "GetCompanyDetail", description = "Given a company name, get the details about the company including website URL")
    public CompanyDetail getCompanyDetails(String name) {
        var outputConverter = new BeanOutputConverter<>(CompanyDetail.class);

        String userMessage =
                """
                        Get the details including website url and address for the company: {name}.
                        Only provide the stock ticker if the company is public.
                        {format}
                        """;

        PromptTemplate promptTemplate = new PromptTemplate(userMessage, Map.of("name", name, "format",
                outputConverter.getFormat()));
        Prompt prompt = promptTemplate.create();

        LOGGER.info("Prompt: {}", prompt.toString());

        Generation generation = chatModel.call(prompt).getResult();

        CompanyDetail detail = outputConverter.convert(generation.getOutput().getContent());
        LOGGER.info("CompanyDetail: {}", detail);
        return detail;
    }
}
