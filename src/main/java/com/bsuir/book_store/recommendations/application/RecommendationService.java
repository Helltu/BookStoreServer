package com.bsuir.book_store.recommendations.application;

import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderItem;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_SEED_BOOKS = 15;

    private final ContentBasedRecommendationService contentBased;
    private final CoOccurrenceService coOccurrence;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public List<BookDocument> similarBooks(UUID bookId, int topK) {
        return contentBased.findSimilarToBook(bookId, topK, Set.of());
    }

    public List<BookDocument> frequentlyBoughtWith(UUID bookId, int topK) {
        return coOccurrence.findFrequentlyBoughtWith(bookId, topK, Set.of(bookId));
    }

    @Transactional(readOnly = true)
    public List<BookDocument> personalRecommendations(String username, int topK) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return List.of();

        Set<UUID> ownedOrWished = collectOwnedAndWished(user);
        List<UUID> seeds = collectSeeds(user);

        if (seeds.isEmpty()) return List.of();

        List<BookDocument> contentCandidates = contentBased.findForUser(seeds, topK * 2, ownedOrWished);
        List<BookDocument> coCandidates = collectCoOccurrenceCandidates(seeds, topK * 2, ownedOrWished);

        return mergeAndRank(contentCandidates, coCandidates, topK);
    }

    private Set<UUID> collectOwnedAndWished(User user) {
        Set<UUID> excluded = new HashSet<>();
        user.getWishlist().forEach(b -> excluded.add(b.getId()));

        List<Order> orders = orderRepository.findByUserUsernameOrderByCreatedAtDesc(user.getUsername());
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBookId() != null) excluded.add(item.getBookId());
            }
        }
        return excluded;
    }

    private List<UUID> collectSeeds(User user) {
        LinkedHashSet<UUID> seeds = new LinkedHashSet<>();

        List<Order> orders = orderRepository.findByUserUsernameOrderByCreatedAtDesc(user.getUsername());
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBookId() != null) seeds.add(item.getBookId());
                if (seeds.size() >= MAX_SEED_BOOKS) break;
            }
            if (seeds.size() >= MAX_SEED_BOOKS) break;
        }

        if (seeds.size() < MAX_SEED_BOOKS) {
            user.getWishlist().forEach(b -> {
                if (seeds.size() < MAX_SEED_BOOKS) seeds.add(b.getId());
            });
        }

        return new ArrayList<>(seeds);
    }

    private List<BookDocument> collectCoOccurrenceCandidates(List<UUID> seeds, int topK, Set<UUID> excluded) {
        Map<String, BookDocument> deduped = new LinkedHashMap<>();
        for (UUID seed : seeds) {
            List<BookDocument> partial = coOccurrence.findFrequentlyBoughtWith(seed, topK, excluded);
            for (BookDocument b : partial) {
                deduped.putIfAbsent(b.getId(), b);
            }
            if (deduped.size() >= topK) break;
        }
        return new ArrayList<>(deduped.values());
    }

    private List<BookDocument> mergeAndRank(List<BookDocument> content, List<BookDocument> co, int topK) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, BookDocument> byId = new HashMap<>();

        for (int i = 0; i < content.size(); i++) {
            BookDocument b = content.get(i);
            double s = 1.0 - (double) i / Math.max(1, content.size());
            scores.merge(b.getId(), s * 0.6, Double::sum);
            byId.putIfAbsent(b.getId(), b);
        }
        for (int i = 0; i < co.size(); i++) {
            BookDocument b = co.get(i);
            double s = 1.0 - (double) i / Math.max(1, co.size());
            scores.merge(b.getId(), s * 0.4, Double::sum);
            byId.putIfAbsent(b.getId(), b);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> byId.get(e.getKey()))
                .collect(Collectors.toList());
    }
}
