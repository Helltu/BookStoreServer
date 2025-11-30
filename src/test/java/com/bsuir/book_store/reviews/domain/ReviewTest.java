package com.bsuir.book_store.reviews.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewTest {

    @Test
    void shouldCreateValidReview() {
        Review review = Review.leave(new User(), new Book(), 5, "Great book!");

        assertNull(review.getId());
        assertEquals(5, review.getRating());
        assertEquals("Great book!", review.getText());
    }

    @Test
    void shouldFailOnInvalidRating() {
        assertThrows(DomainException.class, () ->
                Review.leave(new User(), new Book(), 6, "Wow")
        );

        assertThrows(DomainException.class, () ->
                Review.leave(new User(), new Book(), 0, "Bad")
        );
    }

    @Test
    void shouldFailOnEmptyText() {
        assertThrows(DomainException.class, () ->
                Review.leave(new User(), new Book(), 5, "")
        );
    }
}