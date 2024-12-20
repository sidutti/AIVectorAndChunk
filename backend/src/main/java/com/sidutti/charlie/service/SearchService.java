package com.sidutti.charlie.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import com.sidutti.charlie.model.SearchResults;
import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.EmbeddingUtils;
import org.springframework.ai.vectorstore.ElasticsearchAiSearchFilterExpressionConverter;
import org.springframework.ai.vectorstore.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimilarityFunction;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

import static java.lang.Math.sqrt;

@Component
public class SearchService {
    private final EmbeddingModel embeddingModel;
    private final ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
    private final FilterExpressionConverter filterExpressionConverter;
    private final ElasticsearchAsyncClient elasticsearchAsyncClient;


    public SearchService(EmbeddingModel embeddingModel,
                         ElasticsearchAsyncClient elasticsearchAsyncClient,
                         ElasticsearchVectorStoreProperties properties) {
        this.embeddingModel = embeddingModel;
        this.elasticsearchAsyncClient = elasticsearchAsyncClient;
        filterExpressionConverter = new ElasticsearchAiSearchFilterExpressionConverter();


        if (StringUtils.hasText(properties.getIndexName())) {
            options.setIndexName(properties.getIndexName());
        }
        if (properties.getDimensions() != null) {
            options.setDimensions(properties.getDimensions());
        }
        if (properties.getSimilarity() != null) {
            options.setSimilarity(properties.getSimilarity());
        }
    }

    public Flux<SearchResults> similaritySearch(SearchRequest request) {
        return this.doSimilaritySearch(request);
    }

    public Flux<SearchResults> doSimilaritySearch(SearchRequest searchRequest) {
        Assert.notNull(searchRequest, "The search request must not be null.");
        float threshold = (float) searchRequest.getSimilarityThreshold();
        // reverting l2_norm distance to its original value
        if (options.getSimilarity().equals(SimilarityFunction.l2_norm)) {
            threshold = 1 - threshold;
        }
        final float finalThreshold = threshold;
        float[] vectors = this.embeddingModel.embed(searchRequest.getQuery());


        return Mono.fromFuture(elasticsearchAsyncClient.search(
                        buildQuery(searchRequest, vectors, finalThreshold), Document.class))
                .flatMapMany(response -> Flux.fromIterable(response.hits().hits()))
                .map(this::toDocument);

    }

    private Function<co.elastic.clients.elasticsearch.core.SearchRequest.Builder,
            ObjectBuilder<co.elastic.clients.elasticsearch.core.SearchRequest>> buildQuery(SearchRequest searchRequest,
                                                                                           float[] vectors,
                                                                                           float finalThreshold) {
        MatchQuery matchQuery = new MatchQuery.Builder()
                .field("content")
                .query(searchRequest.getQuery())
                .boost(1.0f)
                .build();


        return sr -> sr.index(options.getIndexName())
                .query(matchQuery._toQuery())
                .knn(knn -> buildKnnQuery(searchRequest, vectors, finalThreshold, knn));
    }

    private KnnSearch.Builder buildKnnQuery(SearchRequest searchRequest, float[] vectors, float finalThreshold, KnnSearch.Builder knn) {
        return knn.queryVector(EmbeddingUtils.toList(vectors))
                .similarity(finalThreshold)
                .k(searchRequest.getTopK())
                .field("embedding")
                .numCandidates((int) (1.5 * searchRequest.getTopK()))
                .filter(fl -> fl.queryString(
                        qs -> qs.query(getElasticsearchQueryString(searchRequest.getFilterExpression()))));
    }

    private String getElasticsearchQueryString(Filter.Expression filterExpression) {
        return Objects.isNull(filterExpression) ? "*"
                : this.filterExpressionConverter.convertExpression(filterExpression);

    }

    private SearchResults toDocument(Hit<Document> hit) {
        assert hit.score() != null;
        float v = calculateDistance(hit.score().floatValue());
        Document document = hit.source();
        assert document != null;
        document.getMetadata().put("distance", v);
        return new SearchResults(document.getContent(), document.getFormattedContent(), document.getId(), v);
    }

    // more info on score/distance calculation
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html#knn-similarity-search
    private float calculateDistance(Float score) {
        if (Objects.requireNonNull(options.getSimilarity()) == SimilarityFunction.l2_norm) {// the returned value of l2_norm is the opposite of the other functions
            // (closest to zero means more accurate), so to make it consistent
            // with the other functions the reverse is returned applying a "1-"
            // to the standard transformation
            return (float) (1 - (sqrt((1 / score) - 1)));
            // cosine and dot_product
        }
        return (2 * score) - 1;
    }


}
