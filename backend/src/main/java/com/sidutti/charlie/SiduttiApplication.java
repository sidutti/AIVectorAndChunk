package com.sidutti.charlie;

import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@EnableReactiveMongoRepositories(basePackages = "com.sidutti.charlie.repository")
@SpringBootApplication(exclude = OllamaAutoConfiguration.class)
public class SiduttiApplication {

        public static void main(String[] args) {
                SpringApplication.run(SiduttiApplication.class, args);
        }

}
