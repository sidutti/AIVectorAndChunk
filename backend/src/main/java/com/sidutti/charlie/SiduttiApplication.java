package com.sidutti.charlie;

import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;


@EnableReactiveElasticsearchRepositories(basePackages = "com.sidutti.charlie.repository.elastic")
@SpringBootApplication(exclude = {ElasticsearchVectorStoreAutoConfiguration.class})
public class SiduttiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiduttiApplication.class, args);
    }

}
