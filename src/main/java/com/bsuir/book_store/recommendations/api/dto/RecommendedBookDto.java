package com.bsuir.book_store.recommendations.api.dto;

import com.bsuir.book_store.catalog.domain.document.BookDocument;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class RecommendedBookDto {
    String id;
    String title;
    List<String> authors;
    List<String> genres;
    BigDecimal price;
    Double averageRating;
    Integer totalReviews;
    String coverUrl;

    public static RecommendedBookDto from(BookDocument b) {
        return RecommendedBookDto.builder()
                .id(b.getId())
                .title(b.getTitle())
                .authors(b.getAuthors())
                .genres(b.getGenres())
                .price(b.getPrice())
                .averageRating(b.getAverageRating())
                .totalReviews(b.getTotalReviews())
                .coverUrl(b.getCoverUrl())
                .build();
    }
}
