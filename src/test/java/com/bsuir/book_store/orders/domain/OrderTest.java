package com.bsuir.book_store.orders.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        user = new User();
        book = Book.builder()
                .id(UUID.randomUUID())
                .title("Test Book")
                .cost(new BigDecimal("15.00"))
                .stockQuantity(10)
                .build();
    }

    @Test
    void shouldCalculateTotalCostCorrectly() {
        Order order = Order.create(user);
        Book book2 = Book.builder().id(UUID.randomUUID()).title("Book 2").cost(new BigDecimal("20.00")).stockQuantity(5).build();

        order.addItem(book, 2);
        order.addItem(book2, 1);

        assertEquals(new BigDecimal("50.00"), order.getTotalCost());
    }

    @Test
    void shouldReserveStockOnAddItem() {
        Order order = Order.create(user);
        order.addItem(book, 3);

        assertEquals(7, book.getStockQuantity());
    }

    @Test
    void shouldThrowWhenQuantityIsZeroOrNegative() {
        Order order = Order.create(user);

        assertThrows(DomainException.class, () -> order.addItem(book, 0));
        assertThrows(DomainException.class, () -> order.addItem(book, -1));
    }

    @Test
    void shouldThrowWhenInsufficientStock() {
        Order order = Order.create(user);

        assertThrows(DomainException.class, () -> order.addItem(book, 999));
    }

    @Test
    void shouldAllowValidStatusTransitionsFromNew() {
        Order order = Order.create(user);
        assertEquals(OrderStatus.NEW, order.getStatus());

        order.updateStatus(OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }

    @Test
    void shouldAllowCancellationFromNew() {
        Order order = Order.create(user);
        order.addItem(book, 2);
        order.updateStatus(OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, book.getStockQuantity()); // stock released
    }

    @Test
    void shouldNotAllowStatusChangeFromCancelled() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.CANCELLED);

        assertThrows(DomainException.class, () -> order.updateStatus(OrderStatus.PROCESSING));
    }

    @Test
    void shouldNotAllowStatusChangeFromReturned() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        order.requestReturn();
        order.approveReturn();

        assertThrows(DomainException.class, () -> order.updateStatus(OrderStatus.PROCESSING));
    }

    @Test
    void shouldNotAllowInvalidTransitionFromProcessing() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);

        assertThrows(DomainException.class, () -> order.updateStatus(OrderStatus.DELIVERED));
    }

    @Test
    void cancelByUserShouldOnlyWorkFromNew() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);

        assertThrows(DomainException.class, order::cancelByUser);
    }

    @Test
    void cancelByUserShouldReleaseStock() {
        Order order = Order.create(user);
        order.addItem(book, 4);
        order.cancelByUser();

        assertEquals(10, book.getStockQuantity());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void requestReturnShouldOnlyWorkFromDelivered() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);

        assertThrows(DomainException.class, order::requestReturn);
    }

    @Test
    void requestReturnShouldSetCorrectStatus() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        order.requestReturn();

        assertEquals(OrderStatus.RETURN_REQUESTED, order.getStatus());
    }

    @Test
    void approveReturnShouldReleaseStockAndSetReturned() {
        Order order = Order.create(user);
        order.addItem(book, 3);
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        order.requestReturn();
        order.approveReturn();

        assertEquals(OrderStatus.RETURNED, order.getStatus());
        assertEquals(10, book.getStockQuantity());
    }

    @Test
    void approveReturnShouldFailIfNotReturnRequested() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);

        assertThrows(DomainException.class, order::approveReturn);
    }

    @Test
    void rejectReturnShouldRestoreDeliveredStatus() {
        Order order = Order.create(user);
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        order.requestReturn();
        order.rejectReturn();

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void rejectReturnShouldFailIfNotReturnRequested() {
        Order order = Order.create(user);

        assertThrows(DomainException.class, order::rejectReturn);
    }

    @Test
    void shipOrderShouldRequireProcessingOrFailedStatus() {
        Order order = Order.create(user);
        order.arrangeDelivery("Ivan", "+375291234567", "Minsk", null);

        assertThrows(DomainException.class, () -> order.shipOrder("10:00-12:00", LocalDate.now()));
    }

    @Test
    void shipOrderShouldSetShippedStatus() {
        Order order = Order.create(user);
        order.arrangeDelivery("Ivan", "+375291234567", "Minsk", null);
        order.updateStatus(OrderStatus.PROCESSING);
        order.shipOrder("10:00-12:00", LocalDate.now().plusDays(2));

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("10:00-12:00", order.getDeliveryDetails().getDeliveryTimeSlot());
    }

    @Test
    void guardImmutableShouldBlockCancelledAndReturned() {
        Order cancelled = Order.create(user);
        cancelled.updateStatus(OrderStatus.CANCELLED);
        assertThrows(DomainException.class, cancelled::guardImmutable);

        Order returned = Order.create(new User());
        returned.updateStatus(OrderStatus.PROCESSING);
        returned.updateStatus(OrderStatus.SHIPPED);
        returned.updateStatus(OrderStatus.DELIVERED);
        returned.requestReturn();
        returned.approveReturn();
        assertThrows(DomainException.class, returned::guardImmutable);
    }
}
