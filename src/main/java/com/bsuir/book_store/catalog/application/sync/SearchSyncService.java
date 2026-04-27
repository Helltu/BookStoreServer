package com.bsuir.book_store.catalog.application.sync;

import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchSyncService {

    private final BookElasticRepository elasticRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncBook(Book book) {
        BookDocument doc = BookDocument.builder()
                .id(book.getId().toString())
                .title(book.getTitle())
                .description(book.getDescription())
                .isbn(book.getIsbn())
                .stockQuantity(book.getStockQuantity())
                .price(book.getCost())
                .averageRating(book.getAverageRating() != null ? book.getAverageRating() : 0.0)
                .totalReviews(book.getTotalReviews() != null ? book.getTotalReviews() : 0)
                .authors(book.getAuthors().stream().map(a -> a.getName()).toList())
                .genres(book.getGenres().stream().map(g -> g.getName()).toList())
                .publisher(book.getPublisher() != null ? book.getPublisher().getName() : null)
                .keywords(book.getKeywords().stream().toList())
                .coverUrl(book.getCoverImage() != null ? book.getCoverImage().getUrl() : null)
                .previewUrls(book.getPreviewImages() != null ? book.getPreviewImages().stream().map(com.bsuir.book_store.catalog.domain.model.Image::getUrl).toList() : java.util.List.of())
                .build();

        elasticRepository.save(doc);
    }
}