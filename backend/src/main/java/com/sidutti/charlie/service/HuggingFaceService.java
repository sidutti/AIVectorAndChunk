package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Document;
import com.sidutti.charlie.model.Root;
import com.sidutti.charlie.repository.DocumentRepository;
import com.sidutti.charlie.vector.mongo.MongoDBAtlasVectorStore;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HuggingFaceService {
        private final MongoDBAtlasVectorStore vectorStore;
        private final WebClient webClient;

        private final DocumentRepository documentRepository;
        public HuggingFaceService(MongoDBAtlasVectorStore vectorStore, WebClient webClient, DocumentRepository documentRepository) {
                this.vectorStore = vectorStore;
                this.webClient = webClient;
                this.documentRepository = documentRepository;
        }
        public Flux<Document> createEmbeddingsFromHuggingFace(int pageNumber, int numberOfRows) {
                return webClient
                                .get()
                                .uri("https://datasets-server.huggingface.co/rows?dataset=ibivibiv%2Fmath_instruct&config=default&split=train&offset="+pageNumber+"&length="+numberOfRows)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(Root.class)
                                .map(Root::rows)
                                .flatMapIterable(list -> list)
                                .doOnError(Throwable::printStackTrace)
                                .map(this::createDocument)
                                .flatMap(documentRepository::save);
        }
        private Document createDocument(Root.RootRow rootRow) {
                String finalValue = rootRow.row().instruction().concat(rootRow.row().output());
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", rootRow.row().output());
                metadata.put("description", rootRow.row().instruction());
                org.springframework.ai.document.Document document = new org.springframework.ai.document.Document(finalValue, metadata);
                List<Double> embedding = vectorStore.generateEmbeddings(document);
               return new Document(new ObjectId(), metadata, finalValue, embedding);

        }
}
