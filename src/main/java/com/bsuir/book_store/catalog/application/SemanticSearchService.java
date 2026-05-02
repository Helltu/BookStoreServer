package com.bsuir.book_store.catalog.application;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.bsuir.book_store.catalog.application.embedding.EmbeddingService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;

    public List<BookDocument> semanticSearch(String naturalQuery, int topK) {
        if (naturalQuery == null || naturalQuery.isBlank()) return List.of();

        float[] queryVector = embeddingService.embed(naturalQuery);
        if (queryVector == null) return List.of();

        List<Float> vectorList = new ArrayList<>(queryVector.length);
        for (float v : queryVector) vectorList.add(v);

        KnnSearch knn = KnnSearch.of(k -> k
                .field("descriptionEmbedding")
                .queryVector(vectorList)
                .k(topK)
                .numCandidates(Math.max(50, topK * 10))
        );

        NativeQuery query = NativeQuery.builder()
                .withKnnSearches(List.of(knn))
                .build();

        SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);
        return hits.stream().map(h -> h.getContent()).toList();
    }
}
