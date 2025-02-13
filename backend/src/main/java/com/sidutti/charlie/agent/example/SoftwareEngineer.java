package com.sidutti.charlie.agent.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

@Worker(goal = "Interpret and answer questions about software code")
public class SoftwareEngineer extends WorkerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareEngineer.class);

    @Feature(name = "SchemaGenerator", description = "Given the fully qualified name of a class, generate a JSON schema for it")
    public String generateSchema(String className) {
        LOGGER.info("Generating schema for: {}", className);
        Class<?> aClass;
        try {
            aClass = Class.forName(className, false, this.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found", e);
            return "Class not found";
        }
        return generateSchema(aClass);
    }

    /*
    Stolen from BeanOutputParser
     */
    private String generateSchema(Class<?> clazz) {

        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfigBuilder configBuilder =
                (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON))
                        .with(jacksonModule);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonNode = generator.generateSchema(clazz, new Type[0]);
        ObjectWriter objectWriter = (new ObjectMapper())
                .writer((new DefaultPrettyPrinter())
                        .withObjectIndenter((new DefaultIndenter())
                                .withLinefeed(System.lineSeparator())));

        String jsonSchema;
        try {
            jsonSchema = objectWriter.writeValueAsString(jsonNode);
        } catch (JsonProcessingException var8) {
            throw new RuntimeException("Could not pretty print json schema for " + clazz, var8);
        }
        return String.format("\n```%s```\n", jsonSchema);
    }
}
