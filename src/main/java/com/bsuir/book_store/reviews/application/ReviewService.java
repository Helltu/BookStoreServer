package com.bsuir.book_store.reviews.application;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.reviews.api.dto.CreateReviewRequest;
import com.bsuir.book_store.reviews.domain.Review;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
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
    private final SearchSyncService searchSyncService;
    private final OrderRepository orderRepository;

    @Transactional
    public UUID addReview(CreateReviewRequest request, String username) {
        if (!orderRepository.existsDeliveredOrderForUserAndBook(username, request.getBookId())) {
            throw new DomainException("Оставить отзыв можно только после получения или возврата книги");
        }

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

        review = reviewRepository.save(review);
        reviewRepository.flush(); // Принудительно сбрасываем в БД перед пересчетом
        
        updateBookRating(book);
        return review.getId();
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new DomainException("Review not found"));
        Book book = review.getBook();
        
        reviewRepository.delete(review);
        reviewRepository.flush(); // Принудительно удаляем из БД перед пересчетом
        updateBookRating(book);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsForBook(UUID bookId) {
        return reviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId);
    }

    @Transactional(readOnly = true)
    public boolean canReview(UUID bookId, String username) {
        return orderRepository.existsDeliveredOrderForUserAndBook(username, bookId)
                && !reviewRepository.existsByBookIdAndUserUsername(bookId, username);
    }

    private void updateBookRating(Book book) {
        long count = reviewRepository.countByBookId(book.getId());
        Double avg = reviewRepository.getAverageRatingByBookId(book.getId());
        
        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
        
        book.updateRating(roundedAvg, (int) count);
        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }
}