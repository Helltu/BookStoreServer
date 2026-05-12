package com.bsuir.book_store.catalog.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    private Book book;

    @BeforeEach
    void setUp() {
        book = Book.builder()
                .id(UUID.randomUUID())
                .title("Test Book")
                .cost(new BigDecimal("50.00"))
                .stockQuantity(10)
                .build();
    }

    @Test
    void reserveStockShouldDecrementQuantity() {
        book.reserveStock(3);
        assertEquals(7, book.getStockQuantity());
    }

    @Test
    void reserveStockShouldThrowWhenInsufficientStock() {
        assertThrows(DomainException.class, () -> book.reserveStock(11));
    }

    @Test
    void reserveStockShouldThrowOnZeroOrNegative() {
        assertThrows(DomainException.class, () -> book.reserveStock(0));
        assertThrows(DomainException.class, () -> book.reserveStock(-5));
    }

    @Test
    void reserveStockShouldAllowExactAmount() {
        book.reserveStock(10);
        assertEquals(0, book.getStockQuantity());
    }

    @Test
    void releaseStockShouldIncrementQuantity() {
        book.reserveStock(5);
        book.releaseStock(5);
        assertEquals(10, book.getStockQuantity());
    }

    @Test
    void updatePriceShouldChangePrice() {
        book.updatePrice(new BigDecimal("99.99"));
        assertEquals(new BigDecimal("99.99"), book.getCost());
    }

    @Test
    void updatePriceShouldThrowOnNegative() {
        assertThrows(DomainException.class, () -> book.updatePrice(new BigDecimal("-1.00")));
    }

    @Test
    void updatePriceShouldAllowZero() {
        assertDoesNotThrow(() -> book.updatePrice(BigDecimal.ZERO));
    }

    @Test
    void softDeleteShouldMarkAsDeleted() {
        assertFalse(book.isDeleted());
        book.softDelete();
        assertTrue(book.isDeleted());
    }

    @Test
    void restoreShouldClearDeletedAt() {
        book.softDelete();
        book.restore();
        assertFalse(book.isDeleted());
    }

    @Test
    void updateRatingShouldSetAverageAndCount() {
        book.updateRating(4.3, 15);
        assertEquals(4.3, book.getAverageRating());
        assertEquals(15, book.getTotalReviews());
    }

    @Test
    void addKeywordShouldAddNonBlankKeyword() {
        book.addKeyword("fantasy");
        assertTrue(book.getKeywords().contains("fantasy"));
    }

    @Test
    void addKeywordShouldIgnoreBlankOrNull() {
        book.addKeyword(null);
        book.addKeyword("  ");
        // keywords set may be null or empty when no valid keywords added
        assertTrue(book.getKeywords() == null || book.getKeywords().isEmpty());
    }

    @Test
    void removeKeywordShouldDeleteKeyword() {
        book.addKeyword("sci-fi");
        book.removeKeyword("sci-fi");
        assertFalse(book.getKeywords().contains("sci-fi"));
    }
}
