package com.bsuir.book_store.catalog.application;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bsuir.book_store.catalog.api.dto.BookSearchCriteria;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final ElasticsearchOperations elasticsearchOperations;

    public BookDocument getBookById(String id) {
        BookDocument doc = elasticsearchOperations.get(id, BookDocument.class);
        if (doc == null) throw new com.bsuir.book_store.shared.exception.DomainException("Книга не найдена");
        return doc;
    }

    public Page<BookDocument> search(BookSearchCriteria criteria, Pageable pageable) {
        // 1. Очистка сортировки: строгий БЕЛЫЙ СПИСОК (Whitelist) разрешенных полей
        List<Sort.Order> validOrders = new ArrayList<>();
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                String prop = order.getProperty();
                if ("price".equals(prop)) {
                    validOrders.add(order);
                } else if ("title".equals(prop) || "publisher".equals(prop) || "authors".equals(prop)) {
                    validOrders.add(order.withProperty(prop + ".keyword"));
                }
                // Любой другой мусор от клиента (включая "[]") будет проигнорирован
            }
        }
        Pageable safePageable = validOrders.isEmpty() 
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) 
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(validOrders));

        // 2. Проверка наличия фильтров
        boolean hasQuery = criteria.getQuery() != null && !criteria.getQuery().isBlank();
        boolean hasMinPrice = criteria.getMinPrice() != null;
        boolean hasMaxPrice = criteria.getMaxPrice() != null;
        boolean hasGenres = criteria.getGenres() != null && !criteria.getGenres().isEmpty();
        boolean hasAuthors = criteria.getAuthors() != null && !criteria.getAuthors().isEmpty();
        boolean hasPublisher = criteria.getPublisher() != null && !criteria.getPublisher().isBlank();

        Query query;
        if (!hasQuery && !hasMinPrice && !hasMaxPrice && !hasGenres && !hasAuthors && !hasPublisher) {
            // Если фильтров нет, возвращаем все документы (match_all)
            query = Query.of(q -> q.matchAll(m -> m));
        } else {
            query = Query.of(q -> q.bool(b -> {
                if (hasQuery) {
                    b.must(m -> m.multiMatch(mm -> mm
                            .query(criteria.getQuery())
                            .fields("title^5", "authors^3", "keywords^2", "description^2", "genres^1", "publisher^1")
                            .fuzziness("AUTO")
                    ));
                }

                if (hasMinPrice) {
                    b.filter(f -> f.range(r -> r.number(n -> n.field("price").gte(criteria.getMinPrice().doubleValue()))));
                }

                if (hasMaxPrice) {
                    b.filter(f -> f.range(r -> r.number(n -> n.field("price").lte(criteria.getMaxPrice().doubleValue()))));
                }

                if (hasGenres) {
                    List<FieldValue> values = criteria.getGenres().stream().map(FieldValue::of).toList();
                    b.filter(f -> f.terms(t -> t.field("genres.keyword").terms(tt -> tt.value(values))));
                }

                if (hasAuthors) {
                    List<FieldValue> values = criteria.getAuthors().stream().map(FieldValue::of).toList();
                    b.filter(f -> f.terms(t -> t.field("authors.keyword").terms(tt -> tt.value(values))));
                }

                if (hasPublisher) {
                    b.filter(f -> f.term(t -> t.field("publisher.keyword").value(criteria.getPublisher())));
                }

                return b;
            }));
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(safePageable)
                .build();

        SearchHits<BookDocument> searchHits = elasticsearchOperations.search(nativeQuery, BookDocument.class);

        List<BookDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(content, safePageable, searchHits.getTotalHits());
    }
}