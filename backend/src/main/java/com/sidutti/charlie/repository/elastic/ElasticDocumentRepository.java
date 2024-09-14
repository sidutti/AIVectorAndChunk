package com.sidutti.charlie.repository.elastic;

import com.sidutti.charlie.model.Document;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface ElasticDocumentRepository extends ReactiveElasticsearchRepository<Document, String> {
}
