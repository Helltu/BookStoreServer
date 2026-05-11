package com.bsuir.book_store.recommendations.application;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentBasedRecommendationService {

    private static final int CANDIDATE_MULTIPLIER = 4;

    private final BookElasticRepository bookElasticRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public List<BookDocument> findSimilarToBook(UUID bookId, int topK, Set<UUID> excluded) {
        BookDocument source = bookElasticRepository.findById(bookId.toString()).orElse(null);
        if (source == null || source.getDescriptionEmbedding() == null) {
            return List.of();
        }
        Set<UUID> exclusions = mergeWith(excluded, bookId);
        return knnSearch(source.getDescriptionEmbedding(), topK, exclusions);
    }

    public List<BookDocument> findForUser(Collection<UUID> seedBookIds, int topK, Set<UUID> excluded) {
        if (seedBookIds == null || seedBookIds.isEmpty()) return List.of();

        List<float[]> vectors = new ArrayList<>();
        for (UUID id : seedBookIds) {
            BookDocument doc = bookElasticRepository.findById(id.toString()).orElse(null);
            if (doc != null && doc.getDescriptionEmbedding() != null) {
                vectors.add(doc.getDescriptionEmbedding());
            }
        }
        if (vectors.isEmpty()) return List.of();

        float[] centroid = averageVector(vectors);
        return knnSearch(centroid, topK, excluded == null ? Set.of() : excluded);
    }

    private List<BookDocument> knnSearch(float[] vector, int topK, Set<UUID> excluded) {
        int fetchSize = Math.max(topK + excluded.size(), topK * CANDIDATE_MULTIPLIER);

        List<Float> vectorList = new ArrayList<>(vector.length);
        for (float v : vector) vectorList.add(v);

        KnnSearch knn = KnnSearch.of(k -> k
                .field("descriptionEmbedding")
                .queryVector(vectorList)
                .k(fetchSize)
                .numCandidates(Math.max(100, fetchSize * 5))
        );

        NativeQuery query = NativeQuery.builder()
                .withKnnSearches(List.of(knn))
                .build();

        SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);

        return hits.stream()
                .map(h -> h.getContent())
                .filter(b -> b.getStockQuantity() != null && b.getStockQuantity() > 0)
                .filter(b -> b.getDeletedAt() == null)
                .filter(b -> !excluded.contains(toUuid(b.getId())))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private float[] averageVector(List<float[]> vectors) {
        int dim = vectors.get(0).length;
        float[] sum = new float[dim];
        for (float[] v : vectors) {
            for (int i = 0; i < dim; i++) sum[i] += v[i];
        }
        for (int i = 0; i < dim; i++) sum[i] /= vectors.size();
        return sum;
    }

    private Set<UUID> mergeWith(Set<UUID> base, UUID extra) {
        if (base == null || base.isEmpty()) return Collections.singleton(extra);
        Set<UUID> result = new java.util.HashSet<>(base);
        result.add(extra);
        return result;
    }

    private UUID toUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
