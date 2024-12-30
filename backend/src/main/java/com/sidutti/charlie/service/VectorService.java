package com.sidutti.charlie.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class VectorService {
    private final ElasticsearchAsyncClient client;
    private final String indexName = "irs-ai-index";

    public VectorService(ElasticsearchAsyncClient client) {
        this.client = client;
    }

    public Mono<IndexResponse> saveDocument(ElasticsearchVectorStore.ElasticSearchDocument document) {
        return Mono.fromFuture(
                client.index(index -> index
                        .id(document.id())
                        .document(document)
                        .index("google-ai-index")
                ));
    }
    public Mono<IndexResponse> saveDocument(Mono<ElasticsearchVectorStore.ElasticSearchDocument> input) {
        return input.flatMap(document ->
                Mono.fromFuture(
                client.index(index -> index
                        .id(document.id())
                        .document(document)
                        .index("google-ai-index")
                )));
    }


}
