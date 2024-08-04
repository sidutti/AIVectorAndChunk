package com.sidutti.charlie.controller;

import com.sidutti.charlie.model.Basics;
import com.sidutti.charlie.repository.mongo.BasicsRepository;
import com.sidutti.charlie.service.FileReaderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
public class IMDBDataLoadController {
        private final FileReaderService fileReaderService;
        private final BasicsRepository basicsRepository;

        public IMDBDataLoadController(FileReaderService fileReaderService, BasicsRepository basicsRepository) {
                this.fileReaderService = fileReaderService;
                this.basicsRepository = basicsRepository;
        }

        @Bean
        public RouterFunction<ServerResponse> getRoutes() {
                return route(GET("/dataLoad/Basics"), this::readAndLoadBasics);
        }

        private Mono<ServerResponse> readAndLoadBasics(ServerRequest serverRequest) {
                String fileName = serverRequest.queryParam("fileName").orElseThrow();
                return ok().body(fileReaderService.readFile(fileName)
                                .map(fileReaderService::splitLine)
                                .map(fileReaderService::getBasics)
                                .flatMap(basicsRepository::save), Basics.class);

        }
}
