package com.bsuir.book_store.reviews.api.dto;

import lombok.Data;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на добавление отзыва")
public class CreateReviewRequest {
    @Schema(description = "ID книги", example = "")
    private UUID bookId;

    @Schema(description = "Оценка (от 1 до 5)")
    private Integer rating;

    @Schema(description = "Текст отзыва", example = "")
    private String text;
}