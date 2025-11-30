package com.bsuir.book_store.reviews.api;

import com.bsuir.book_store.reviews.api.dto.CreateReviewRequest;
import com.bsuir.book_store.reviews.application.ReviewService;
import com.bsuir.book_store.reviews.domain.Review;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Управление отзывами и рейтингами")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Оставить отзыв",
            description = "Добавляет отзыв к книге от имени текущего пользователя. Требуется авторизация."
    )
    @PostMapping
    public ResponseEntity<UUID> addReview(
            @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reviewService.addReview(request, userDetails.getUsername()));
    }

    @Operation(
            summary = "Получить отзывы книги",
            description = "Возвращает список всех отзывов для указанной книги, отсортированных по дате."
    )
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<Review>> getBookReviews(@PathVariable UUID bookId) {
        return ResponseEntity.ok(reviewService.getReviewsForBook(bookId));
    }

    @Operation(
            summary = "Удалить отзыв (Модерация)",
            description = "Удаляет отзыв по ID. Доступно только для роли MANAGER."
    )
    @DeleteMapping("/{id}")
    @IsManager
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}