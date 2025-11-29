package com.bsuir.book_store.reviews.api;

import com.bsuir.book_store.reviews.api.dto.CreateReviewRequest;
import com.bsuir.book_store.reviews.application.ReviewService;
import com.bsuir.book_store.reviews.domain.Review;
import com.bsuir.book_store.shared.security.annotations.IsManager;
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
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<UUID> addReview(
            @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reviewService.addReview(request, userDetails.getUsername()));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<Review>> getBookReviews(@PathVariable UUID bookId) {
        return ResponseEntity.ok(reviewService.getReviewsForBook(bookId));
    }

    @DeleteMapping("/{id}")
    @IsManager
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}