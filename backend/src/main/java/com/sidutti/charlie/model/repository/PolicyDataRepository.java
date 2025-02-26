package com.sidutti.charlie.model.repository;

import com.sidutti.charlie.model.PolicyData;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Mono;

public interface PolicyDataRepository extends ReactiveElasticsearchRepository<PolicyData, String> {
    Mono<PolicyData> findByName(String name);
}
