package com.bsuir.book_store.reviews.api.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateReviewRequest {
    private UUID bookId;
    private Integer rating;
    private String text;
}