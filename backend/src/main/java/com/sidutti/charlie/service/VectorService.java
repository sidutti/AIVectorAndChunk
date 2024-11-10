package com.sidutti.charlie.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class VectorService {
    private final ElasticsearchAsyncClient client;
    private final String indexName = "spring-ai-document-index";
    public VectorService(ElasticsearchAsyncClient client) {
        this.client = client;
    }

    public Mono<IndexResponse> saveDocument(Document document) {
        return Mono.fromFuture(client.index(index -> index.id(document.getId()).document(document)));
    }

    public Mono<BulkResponse> saveDocument(List<Document> documents) {
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        for (Document document : documents) {
            bulkRequestBuilder.operations(op -> op
                    .index(idx -> idx.index(indexName).id(document.getId()).document(document)));
        }
        return Mono.fromFuture(client.bulk(bulkRequestBuilder.build()));
    }
}
