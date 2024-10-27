package com.sidutti.charlie.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class VectorService {
private final ElasticsearchAsyncClient client;

    public VectorService(ElasticsearchAsyncClient client) {
        this.client = client;
    }

    public Mono<IndexResponse> saveDocument(Document document) {
        return Mono.fromFuture(client.index(index -> index.id(document.getId()).document(document)));
    }
}
