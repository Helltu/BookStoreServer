package com.bsuir.book_store.orders.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldCalculateTotalCostCorrectly() {
        User user = new User();
        Order order = Order.create(user);

        Book book1 = Book.builder().id(UUID.randomUUID()).title("Book 1").cost(new BigDecimal("10.00")).stockQuantity(10).build();
        Book book2 = Book.builder().id(UUID.randomUUID()).title("Book 2").cost(new BigDecimal("20.00")).stockQuantity(5).build();

        order.addItem(book1, 2);
        order.addItem(book2, 1);

        assertEquals(new BigDecimal("40.00"), order.getTotalCost());
    }

    @Test
    void shouldNotAllowStatusChangeFromCancelled() {
        User user = new User();
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.CANCELLED);

        assertThrows(DomainException.class, () -> order.updateStatus(OrderStatus.SHIPPED));
    }
}