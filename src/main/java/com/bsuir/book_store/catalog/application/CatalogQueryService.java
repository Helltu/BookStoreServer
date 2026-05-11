package com.bsuir.book_store.catalog.application;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bsuir.book_store.catalog.api.dto.BookSearchCriteria;
import com.bsuir.book_store.catalog.api.dto.LanguageOption;
import com.bsuir.book_store.catalog.api.dto.PriceRangeResponse;
import com.bsuir.book_store.catalog.api.dto.YearRangeResponse;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.domain.model.AgeRating;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.BookFormat;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookRepository bookRepository;

    private BookDocument toDocument(Book book) {
        return BookDocument.builder()
                .id(book.getId().toString())
                .title(book.getTitle())
                .description(book.getDescription())
                .isbn(book.getIsbn())
                .stockQuantity(book.getStockQuantity())
                .price(book.getCost())
                .averageRating(book.getAverageRating())
                .totalReviews(book.getTotalReviews())
                .authors(book.getAuthors().stream().map(a -> a.getName()).toList())
                .genres(book.getGenres().stream().map(g -> g.getName()).toList())
                .publisher(book.getPublisher() != null ? book.getPublisher().getName() : null)
                .keywords(book.getKeywords() != null ? book.getKeywords().stream().toList() : java.util.List.of())
                .pagesCount(book.getPagesCount())
                .format(book.getFormat())
                .weight(book.getWeight())
                .dimensions(book.getDimensions())
                .ageRating(book.getAgeRating())
                .publicationYear(book.getPublicationYear())
                .language(book.getLanguage())
                .originalLanguage(book.getOriginalLanguage())
                .createdAt(book.getCreatedAt() != null ? book.getCreatedAt().toInstant() : null)
                .deletedAt(book.getDeletedAt())
                .coverUrl(book.getCoverImage() != null ? book.getCoverImage().getUrl() : null)
                .previewUrls(book.getPreviewImages() != null
                        ? book.getPreviewImages().stream().map(img -> img.getUrl()).toList()
                        : java.util.List.of())
                .build();
    }

    public PriceRangeResponse getPriceRange() {
        return new PriceRangeResponse(bookRepository.findMinPrice(), bookRepository.findMaxPrice());
    }

    public BookDocument getBookById(String id, boolean includeDeleted) {
        BookDocument doc = elasticsearchOperations.get(id, BookDocument.class);
        if (doc != null) return doc;
        if (includeDeleted) {
            return bookRepository.findById(UUID.fromString(id))
                    .map(this::toDocument)
                    .orElseThrow(() -> new com.bsuir.book_store.shared.exception.DomainException("Книга не найдена"));
        }
        throw new com.bsuir.book_store.shared.exception.DomainException("Книга не найдена");
    }

    public Page<BookDocument> search(BookSearchCriteria criteria, Pageable pageable, boolean isManager) {
        // 1. Очистка сортировки: строгий БЕЛЫЙ СПИСОК (Whitelist) разрешенных полей
        List<Sort.Order> validOrders = new ArrayList<>();
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                String prop = order.getProperty();
                if ("price".equals(prop) || "stockQuantity".equals(prop) || "averageRating".equals(prop) || "createdAt".equals(prop)) {
                    validOrders.add(order);
                } else if ("title".equals(prop) || "publisher".equals(prop) || "authors".equals(prop)) {
                    validOrders.add(order.withProperty(prop + ".keyword"));
                }
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
        boolean hasInStock = Boolean.TRUE.equals(criteria.getInStock());
        boolean hasLanguage = criteria.getLanguage() != null && !criteria.getLanguage().isBlank();
        boolean hasFormat = criteria.getFormat() != null;
        boolean hasAgeRating = criteria.getAgeRating() != null;
        boolean hasMinYear = criteria.getMinYear() != null;
        boolean hasMaxYear = criteria.getMaxYear() != null;
        boolean hasMinRating = criteria.getMinRating() != null;
        boolean filterDeleted = isManager && Boolean.TRUE.equals(criteria.getDeleted());

        Query query;
        boolean noFilters = !hasQuery && !hasMinPrice && !hasMaxPrice && !hasGenres && !hasAuthors
                && !hasPublisher && !hasInStock && !hasLanguage && !hasFormat && !hasAgeRating
                && !hasMinYear && !hasMaxYear && !hasMinRating && !filterDeleted && isManager;
        if (noFilters) {
            if (!isManager) {
                query = Query.of(q -> q.bool(b -> b.filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))))));
            } else {
                query = Query.of(q -> q.matchAll(m -> m));
            }
        } else {
            query = Query.of(q -> q.bool(b -> {
                if (hasQuery) {
                    String rawQuery = criteria.getQuery();
                    b.must(m -> m.bool(inner -> {
                        inner.should(s -> s.multiMatch(mm -> mm
                                .query(rawQuery)
                                .fields("title^5", "authors^3", "keywords^2", "description^2", "genres^1", "publisher^1")
                                .fuzziness("AUTO")
                        ));
                        inner.should(s -> s.term(t -> t.field("isbn").value(rawQuery)));
                        if (isValidUUID(rawQuery)) {
                            inner.should(s -> s.ids(i -> i.values(rawQuery)));
                        }
                        inner.minimumShouldMatch("1");
                        return inner;
                    }));
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

                if (hasInStock) {
                    b.filter(f -> f.range(r -> r.number(n -> n.field("stockQuantity").gte(1.0))));
                }

                if (hasLanguage) {
                    b.filter(f -> f.term(t -> t.field("language").value(criteria.getLanguage())));
                }

                if (hasFormat) {
                    b.filter(f -> f.term(t -> t.field("format").value(criteria.getFormat().name())));
                }

                if (hasAgeRating) {
                    b.filter(f -> f.term(t -> t.field("ageRating").value(criteria.getAgeRating().name())));
                }

                if (hasMinYear) {
                    b.filter(f -> f.range(r -> r.number(n -> n.field("publicationYear").gte(criteria.getMinYear().doubleValue()))));
                }

                if (hasMaxYear) {
                    b.filter(f -> f.range(r -> r.number(n -> n.field("publicationYear").lte(criteria.getMaxYear().doubleValue()))));
                }

                if (hasMinRating) {
                    b.filter(f -> f.range(r -> r.number(n -> n.field("averageRating").gte(criteria.getMinRating()))));
                }

                if (filterDeleted) {
                    b.filter(f -> f.exists(e -> e.field("deletedAt")));
                } else if (!isManager) {
                    b.filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))));
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

    public List<String> getAvailableLanguages() {
        return getDistinctKeywordValues("language", 50);
    }

    public List<LanguageOption> getSupportedLanguages() {
        return List.of(
            new LanguageOption("ru", "Русский"),
            new LanguageOption("be", "Белорусский"),
            new LanguageOption("en", "Английский")
        );
    }

    public List<BookFormat> getAvailableFormats() {
        return Arrays.asList(BookFormat.values());
    }

    public List<AgeRating> getAvailableAgeRatings() {
        return Arrays.asList(AgeRating.values());
    }

    public YearRangeResponse getYearRange() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.matchAll(m -> m)))
                .withAggregation("min_year", co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(
                        a -> a.min(m -> m.field("publicationYear"))))
                .withAggregation("max_year", co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(
                        a -> a.max(m -> m.field("publicationYear"))))
                .withMaxResults(0)
                .build();

        SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);

        Integer minYear = null;
        Integer maxYear = null;

        if (hits.getAggregations() != null) {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            ElasticsearchAggregation minAgg = aggs.aggregationsAsMap().get("min_year");
            ElasticsearchAggregation maxAgg = aggs.aggregationsAsMap().get("max_year");
            if (minAgg != null && minAgg.aggregation().getAggregate().isMin()) {
                double v = minAgg.aggregation().getAggregate().min().value();
                if (!Double.isInfinite(v)) minYear = (int) v;
            }
            if (maxAgg != null && maxAgg.aggregation().getAggregate().isMax()) {
                double v = maxAgg.aggregation().getAggregate().max().value();
                if (!Double.isInfinite(v)) maxYear = (int) v;
            }
        }

        return new YearRangeResponse(minYear, maxYear);
    }

    private List<String> getDistinctKeywordValues(String field, int size) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.matchAll(m -> m)))
                .withAggregation(field, co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(
                        a -> a.terms(t -> t.field(field).size(size))))
                .withMaxResults(0)
                .build();

        SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);

        if (hits.getAggregations() == null) return List.of();

        ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
        ElasticsearchAggregation agg = aggs.aggregationsAsMap().get(field);
        if (agg == null || !agg.aggregation().getAggregate().isSterms()) return List.of();

        return agg.aggregation().getAggregate().sterms().buckets().array()
                .stream()
                .map(StringTermsBucket::key)
                .filter(k -> k != null && !k.stringValue().isBlank())
                .map(k -> k.stringValue())
                .toList();
    }

    private boolean isValidUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}