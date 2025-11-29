package com.bsuir.book_store.reviews.application;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.reviews.api.dto.CreateReviewRequest;
import com.bsuir.book_store.reviews.domain.Review;
import com.bsuir.book_store.reviews.infrastructure.ReviewRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID addReview(CreateReviewRequest request, String username) {
        if (reviewRepository.existsByBookIdAndUserUsername(request.getBookId(), username)) {
            throw new DomainException("Вы уже оставили отзыв на эту книгу");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new DomainException("Book not found"));

        Review review = Review.leave(
                user,
                book,
                request.getRating(),
                request.getText()
        );

        return reviewRepository.save(review).getId();
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new DomainException("Review not found");
        }
        reviewRepository.deleteById(reviewId);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsForBook(UUID bookId) {
        return reviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId);
    }
}