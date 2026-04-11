package com.bsuir.book_store.catalog.application;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bsuir.book_store.catalog.api.dto.BookSearchCriteria;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<BookDocument> search(BookSearchCriteria criteria, Pageable pageable) {
        Query query = Query.of(q -> q.bool(b -> {
            // Полнотекстовый поиск (must -> multi_match)
            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
                b.must(m -> m.multiMatch(mm -> mm
                        .query(criteria.getQuery())
                        .fields("title^3", "authors^2", "publisher^2", "keywords^2", "description")
                        .fuzziness("AUTO")
                ));
            }

            // Фильтр: минимальная цена
            if (criteria.getMinPrice() != null) {
                b.filter(f -> f.range(r -> r.number(n -> n.field("price").gte(criteria.getMinPrice().doubleValue()))));
            }

            // Фильтр: максимальная цена
            if (criteria.getMaxPrice() != null) {
                b.filter(f -> f.range(r -> r.number(n -> n.field("price").lte(criteria.getMaxPrice().doubleValue()))));
            }

            // Фильтр: жанры (terms)
            if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
                List<FieldValue> values = criteria.getGenres().stream().map(FieldValue::of).toList();
                b.filter(f -> f.terms(t -> t.field("genres").terms(tt -> tt.value(values))));
            }

            // Фильтр: авторы (terms)
            if (criteria.getAuthors() != null && !criteria.getAuthors().isEmpty()) {
                List<FieldValue> values = criteria.getAuthors().stream().map(FieldValue::of).toList();
                b.filter(f -> f.terms(t -> t.field("authors").terms(tt -> tt.value(values))));
            }

            // Фильтр: издательство (match)
            if (criteria.getPublisher() != null && !criteria.getPublisher().isBlank()) {
                b.filter(f -> f.match(m -> m.field("publisher").query(criteria.getPublisher())));
            }

            return b;
        }));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable)
                .build();

        SearchHits<BookDocument> searchHits = elasticsearchOperations.search(nativeQuery, BookDocument.class);

        List<BookDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }
}