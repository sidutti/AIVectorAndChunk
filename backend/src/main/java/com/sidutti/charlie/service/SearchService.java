package com.sidutti.charlie.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidutti.charlie.model.SearchResults;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.ElasticsearchAiSearchFilterExpressionConverter;
import org.springframework.ai.vectorstore.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
public class SearchService {

    private final EmbeddingModel embeddingModel;
    private final ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
    private final FilterExpressionConverter filterExpressionConverter;
    private final ElasticsearchAsyncClient elasticsearchAsyncClient;

    public SearchService(EmbeddingModel embeddingModel, RestClient restClient) {
        this.embeddingModel = embeddingModel;
        filterExpressionConverter = new ElasticsearchAiSearchFilterExpressionConverter();
        this.elasticsearchAsyncClient = new ElasticsearchAsyncClient(new RestClientTransport(restClient, new JacksonJsonpMapper(
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))));
    }


    public Flux<SearchResults> similaritySearch(SearchRequest searchRequest) {
        Assert.notNull(searchRequest, "The search request must not be null.");
        return similaritySearch(this.embeddingModel.embed(searchRequest.getQuery()), searchRequest.getTopK(),
                Double.valueOf(searchRequest.getSimilarityThreshold()).floatValue(),
                searchRequest.getFilterExpression());
    }

    public Flux<SearchResults> similaritySearch(List<Double> embedding, int topK, double similarityThreshold,
                                                Filter.Expression filterExpression) {
        return similaritySearch(
                new co.elastic.clients.elasticsearch.core.SearchRequest.Builder().index(options.getIndexName())
                        .query(getElasticsearchSimilarityQuery(embedding, filterExpression))
                        .size(topK)
                        .minScore(similarityThreshold)
                        .build());
    }

    private Query getElasticsearchSimilarityQuery(List<Double> embedding, Filter.Expression filterExpression) {
        return Query.of(queryBuilder -> queryBuilder.scriptScore(scriptScoreQueryBuilder -> scriptScoreQueryBuilder
                .query(queryBuilder2 -> queryBuilder2.queryString(queryStringQuerybuilder -> queryStringQuerybuilder
                        .query(getElasticsearchQueryString(filterExpression))))
                .script(scriptBuilder -> scriptBuilder
                        .inline(inlineScriptBuilder -> inlineScriptBuilder.source(ElasticsearchVectorStore.COSINE_SIMILARITY_FUNCTION)
                                .params("query_vector", JsonData.of(embedding))))));
    }

    private String getElasticsearchQueryString(Filter.Expression filterExpression) {
        return Objects.isNull(filterExpression) ? "*"
                : this.filterExpressionConverter.convertExpression(filterExpression);

    }

    private Flux<SearchResults> similaritySearch(co.elastic.clients.elasticsearch.core.SearchRequest searchRequest) {

        return Mono.fromFuture(elasticsearchAsyncClient.search(searchRequest, Document.class))
                .map(ResponseBody::hits)
                .map(HitsMetadata::hits)
                .flatMapIterable(list -> list)
                .map(this::toDocument)
                .onErrorResume(e -> Mono.just(new SearchResults("", "", "", 0)));
    }

    private SearchResults toDocument(Hit<Document> hit) {
        Document document = hit.source();
        float distance = 0;
        if (hit.score() != null) {
            distance = 1 - hit.score().floatValue();
        }
        if (document != null) {
            if (hit.score() != null) {
                document.getMetadata().put("distance", distance);
            }
        }
        assert document != null;
        return new SearchResults(document.getContent(), document.getFormattedContent(), document.getId(), distance);

    }
}
