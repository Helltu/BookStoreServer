package com.bsuir.book_store.reviews.application;

import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.reviews.api.dto.CreateReviewRequest;
import com.bsuir.book_store.reviews.domain.Review;
import com.bsuir.book_store.reviews.infrastructure.ReviewRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.enums.Role;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @Mock private SearchSyncService searchSyncService;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Book book;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        user = User.register("client", "c@test.com", "hash", Role.CLIENT, null, null, null);
        book = Book.builder().id(bookId).title("Book").cost(new BigDecimal("20.00")).stockQuantity(3).build();
    }

    private CreateReviewRequest buildRequest(int rating, String text) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookId(bookId);
        request.setRating(rating);
        request.setText(text);
        return request;
    }

    @Test
    void addReviewShouldSaveAndUpdateRating() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(false);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        Review saved = Review.leave(user, book, 4, "Great");
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(reviewRepository.countByBookId(bookId)).thenReturn(1L);
        when(reviewRepository.getAverageRatingByBookId(bookId)).thenReturn(4.0);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        reviewService.addReview(buildRequest(4, "Great"), "client");

        verify(reviewRepository).save(any(Review.class));
        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void addReviewShouldThrowWhenNoDeliveredOrder() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(false);

        assertThrows(DomainException.class,
                () -> reviewService.addReview(buildRequest(5, "Nice"), "client"));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReviewShouldThrowOnDuplicate() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(true);

        assertThrows(DomainException.class,
                () -> reviewService.addReview(buildRequest(5, "Again"), "client"));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReviewShouldThrowWhenUserNotFound() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(false);
        when(userRepository.findByUsername("client")).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> reviewService.addReview(buildRequest(5, "Nice"), "client"));
    }

    @Test
    void addReviewShouldThrowWhenBookNotFound() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(false);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> reviewService.addReview(buildRequest(5, "Nice"), "client"));
    }

    @Test
    void deleteReviewShouldDeleteAndUpdateRating() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.leave(user, book, 3, "Okay");
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.countByBookId(bookId)).thenReturn(0L);
        when(reviewRepository.getAverageRatingByBookId(bookId)).thenReturn(null);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        reviewService.deleteReview(reviewId);

        verify(reviewRepository).delete(review);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void deleteReviewShouldThrowWhenNotFound() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> reviewService.deleteReview(reviewId));
    }

    @Test
    void getReviewsForBookShouldReturnList() {
        Review review = Review.leave(user, book, 5, "Best");
        when(reviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId)).thenReturn(List.of(review));

        List<Review> result = reviewService.getReviewsForBook(bookId);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getRating());
    }

    @Test
    void canReviewShouldReturnTrueWhenEligible() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(false);

        assertTrue(reviewService.canReview(bookId, "client"));
    }

    @Test
    void canReviewShouldReturnFalseWhenAlreadyReviewed() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(true);

        assertFalse(reviewService.canReview(bookId, "client"));
    }

    @Test
    void canReviewShouldReturnFalseWhenNoDeliveredOrder() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(false);

        assertFalse(reviewService.canReview(bookId, "client"));
    }

    @Test
    void ratingZeroAverageWhenNoReviews() {
        when(orderRepository.existsDeliveredOrderForUserAndBook("client", bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserUsername(bookId, "client")).thenReturn(false);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        Review saved = Review.leave(user, book, 5, "Good");
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewRepository.countByBookId(bookId)).thenReturn(1L);
        when(reviewRepository.getAverageRatingByBookId(bookId)).thenReturn(null); // no avg yet
        when(bookRepository.save(any())).thenReturn(book);

        reviewService.addReview(buildRequest(5, "Good"), "client");

        verify(bookRepository).save(argThat(b -> b.getAverageRating() == 0.0 && b.getTotalReviews() == 1));
    }
}
