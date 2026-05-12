package com.bsuir.book_store.reviews.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewTest {

    @Test
    void shouldCreateValidReview() {
        Review review = Review.leave(new User(), new Book(), 4, "Good book!");

        assertNull(review.getId());
        assertEquals(4, review.getRating());
        assertEquals("Good book!", review.getText());
    }

    @Test
    void shouldAcceptBoundaryRatings() {
        assertDoesNotThrow(() -> Review.leave(new User(), new Book(), 1, "Worst"));
        assertDoesNotThrow(() -> Review.leave(new User(), new Book(), 5, "Best"));
    }

    @Test
    void shouldFailOnRatingAboveFive() {
        assertThrows(DomainException.class, () -> Review.leave(new User(), new Book(), 6, "Wow"));
    }

    @Test
    void shouldFailOnRatingBelowOne() {
        assertThrows(DomainException.class, () -> Review.leave(new User(), new Book(), 0, "Bad"));
    }

    @Test
    void shouldFailOnNegativeRating() {
        assertThrows(DomainException.class, () -> Review.leave(new User(), new Book(), -1, "Negative"));
    }

    @Test
    void shouldFailOnEmptyText() {
        assertThrows(DomainException.class, () -> Review.leave(new User(), new Book(), 5, ""));
    }

    @Test
    void shouldFailOnBlankText() {
        assertThrows(DomainException.class, () -> Review.leave(new User(), new Book(), 5, "   "));
    }

    @Test
    void shouldFailOnNullText() {
        assertThrows(DomainException.class, () -> Review.leave(new User(), new Book(), 5, null));
    }

    @Test
    void shouldUpdateContent() {
        Review review = Review.leave(new User(), new Book(), 3, "Okay");
        review.updateContent(5, "Actually great!");

        assertEquals(5, review.getRating());
        assertEquals("Actually great!", review.getText());
    }

    @Test
    void updateContentShouldValidateRating() {
        Review review = Review.leave(new User(), new Book(), 3, "Okay");

        assertThrows(DomainException.class, () -> review.updateContent(6, "Too high"));
        assertThrows(DomainException.class, () -> review.updateContent(0, "Too low"));
    }

    @Test
    void updateContentShouldValidateText() {
        Review review = Review.leave(new User(), new Book(), 3, "Okay");

        assertThrows(DomainException.class, () -> review.updateContent(3, ""));
        assertThrows(DomainException.class, () -> review.updateContent(3, null));
    }
}
