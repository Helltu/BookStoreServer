package com.bsuir.book_store.recommendations.application;

import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderItem;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.recommendations.domain.BookCoOccurrenceDocument;
import com.bsuir.book_store.recommendations.infrastructure.BookCoOccurrenceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoOccurrenceService {

    private static final Set<OrderStatus> EXCLUDED_STATUSES = EnumSet.of(OrderStatus.CANCELLED);
    private static final int TOP_K_FOR_BOOK = 20;

    private final OrderRepository orderRepository;
    private final BookCoOccurrenceRepository repository;
    private final BookElasticRepository bookElasticRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void initIfEmpty() {
        try {
            if (repository.count() == 0) {
                log.info("Co-occurrence index is empty. Triggering initial computation.");
                recompute();
            }
        } catch (Exception e) {
            log.warn("Initial co-occurrence computation skipped: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void recompute() {
        log.info("Recomputing book co-occurrence matrix...");
        long start = System.currentTimeMillis();

        Map<UUID, Map<UUID, Long>> matrix = new HashMap<>();

        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            if (EXCLUDED_STATUSES.contains(order.getStatus())) continue;

            List<UUID> bookIds = order.getOrderItems().stream()
                    .map(OrderItem::getBookId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            for (int i = 0; i < bookIds.size(); i++) {
                for (int j = 0; j < bookIds.size(); j++) {
                    if (i == j) continue;
                    UUID a = bookIds.get(i);
                    UUID b = bookIds.get(j);
                    matrix.computeIfAbsent(a, k -> new HashMap<>()).merge(b, 1L, Long::sum);
                }
            }
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BookCoOccurrenceDocument.class);
        if (indexOps.exists()) indexOps.delete();
        indexOps.createWithMapping();

        Instant now = Instant.now();
        List<BookCoOccurrenceDocument> docs = matrix.entrySet().stream()
                .map(rowEntry -> BookCoOccurrenceDocument.builder()
                        .bookAId(rowEntry.getKey().toString())
                        .updatedAt(now)
                        .topPairs(rowEntry.getValue().entrySet().stream()
                                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                                .limit(TOP_K_FOR_BOOK)
                                .map(e -> BookCoOccurrenceDocument.Pair.builder()
                                        .bookBId(e.getKey().toString())
                                        .count(e.getValue())
                                        .build())
                                .toList())
                        .build())
                .toList();

        if (!docs.isEmpty()) repository.saveAll(docs);
        log.info("Co-occurrence recomputed: {} source books, {} ms", docs.size(), System.currentTimeMillis() - start);
    }

    public List<BookDocument> findFrequentlyBoughtWith(UUID bookId, int topK, Set<UUID> excluded) {
        BookCoOccurrenceDocument doc = repository.findById(bookId.toString()).orElse(null);
        if (doc == null || doc.getTopPairs() == null || doc.getTopPairs().isEmpty()) return List.of();

        Set<UUID> exclusions = excluded == null ? Set.of() : excluded;

        List<String> ids = doc.getTopPairs().stream()
                .map(BookCoOccurrenceDocument.Pair::getBookBId)
                .filter(id -> !exclusions.contains(toUuid(id)))
                .limit(topK)
                .toList();

        if (ids.isEmpty()) return List.of();

        Map<String, BookDocument> books = new HashMap<>();
        bookElasticRepository.findAllById(ids).forEach(b -> books.put(b.getId(), b));

        return ids.stream()
                .map(books::get)
                .filter(Objects::nonNull)
                .filter(b -> b.getStockQuantity() != null && b.getStockQuantity() > 0)
                .filter(b -> b.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    private UUID toUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
