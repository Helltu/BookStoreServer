package com.bsuir.book_store.catalog.application.sync;

import com.bsuir.book_store.catalog.application.embedding.EmbeddingService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncService {

    private final BookElasticRepository elasticRepository;
    private final EmbeddingService embeddingService;
    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncBook(Book book) {
        float[] embedding = computeEmbedding(book);

        BookDocument doc = BookDocument.builder()
                .id(book.getId().toString())
                .title(book.getTitle())
                .description(book.getDescription())
                .isbn(book.getIsbn())
                .stockQuantity(book.getStockQuantity())
                .price(book.getCost())
                .averageRating(book.getAverageRating() != null ? book.getAverageRating() : 0.0)
                .totalReviews(book.getTotalReviews() != null ? book.getTotalReviews() : 0)
                .totalOrders(orderRepository.countTotalOrderedByBookId(book.getId()))
                .authors(book.getAuthors().stream().map(a -> a.getName()).toList())
                .genres(book.getGenres().stream().map(g -> g.getName()).toList())
                .publisher(book.getPublisher() != null ? book.getPublisher().getName() : null)
                .keywords(book.getKeywords().stream().toList())
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
                .previewUrls(book.getPreviewImages() != null ? book.getPreviewImages().stream().map(com.bsuir.book_store.catalog.domain.model.Image::getUrl).toList() : java.util.List.of())
                .descriptionEmbedding(embedding)
                .build();

        elasticRepository.save(doc);
    }

    private float[] computeEmbedding(Book book) {
        try {
            StringBuilder sb = new StringBuilder();
            if (book.getTitle() != null) sb.append(book.getTitle()).append(". ");
            if (book.getDescription() != null) sb.append(book.getDescription());
            if (book.getGenres() != null) {
                String genres = book.getGenres().stream().map(g -> g.getName()).reduce((a, b) -> a + ", " + b).orElse("");
                if (!genres.isBlank()) sb.append(" Жанры: ").append(genres);
            }
            String text = sb.toString().trim();
            if (text.isBlank()) return null;
            return embeddingService.embed(text);
        } catch (Exception e) {
            log.warn("Failed to compute embedding for book {}: {}", book.getId(), e.getMessage());
            return null;
        }
    }
}
