package com.bsuir.book_store.orders.application;

import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
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
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @Mock private SearchSyncService searchSyncService;
    @Mock private OrderEmailService orderEmailService;

    @InjectMocks
    private OrderService orderService;

    private User userWithProfile;
    private User userWithoutProfile;
    private Book book;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();

        userWithProfile = User.register("client", "c@test.com", "hash", Role.CLIENT, "Ivan", "Petrov", null);
        userWithoutProfile = User.register("noprofile", "n@test.com", "hash", Role.CLIENT, null, null, null);

        book = Book.builder()
                .id(bookId)
                .title("Spring in Action")
                .cost(new BigDecimal("30.00"))
                .stockQuantity(5)
                .build();
    }

    private CreateOrderRequest buildRequest(UUID bookId, int qty) {
        CreateOrderRequest.ItemDto item = new CreateOrderRequest.ItemDto();
        item.setBookId(bookId);
        item.setQuantity(qty);

        CreateOrderRequest.DeliveryDto delivery = new CreateOrderRequest.DeliveryDto();
        delivery.setCustomerName("Ivan");
        delivery.setPhone("+375291234567");
        delivery.setAddress("Minsk, Lenina 1");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item));
        request.setDeliveryDetails(delivery);
        return request;
    }

    @Test
    void createOrderShouldSaveAndNotifyEmail() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(userWithProfile));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(buildRequest(bookId, 2), "client");

        verify(orderRepository).save(any(Order.class));
        verify(searchSyncService).syncBook(book);
        verify(orderEmailService).sendOrderCreated(any(Order.class));
    }

    @Test
    void createOrderShouldThrowWhenUserHasNoProfile() {
        when(userRepository.findByUsername("noprofile")).thenReturn(Optional.of(userWithoutProfile));

        assertThrows(DomainException.class,
                () -> orderService.createOrder(buildRequest(bookId, 1), "noprofile"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderShouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> orderService.createOrder(buildRequest(bookId, 1), "ghost"));
    }

    @Test
    void createOrderShouldThrowWhenBookNotFound() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(userWithProfile));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> orderService.createOrder(buildRequest(bookId, 1), "client"));
    }

    @Test
    void createOrderShouldThrowWhenInsufficientStock() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(userWithProfile));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThrows(DomainException.class,
                () -> orderService.createOrder(buildRequest(bookId, 999), "client"));
    }

    @Test
    void changeStatusShouldUpdateAndSendEmail() {
        Order order = Order.create(userWithProfile);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.changeStatus(UUID.randomUUID(), OrderStatus.PROCESSING);

        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        verify(orderEmailService).sendStatusChanged(eq(order), eq(OrderStatus.PROCESSING));
    }

    @Test
    void changeStatusToCancelledShouldSyncBooks() {
        Order order = Order.create(userWithProfile);
        order.addItem(book, 2);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.changeStatus(UUID.randomUUID(), OrderStatus.CANCELLED);

        verify(searchSyncService, atLeastOnce()).syncBook(any(Book.class));
    }

    @Test
    void changeStatusShouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> orderService.changeStatus(UUID.randomUUID(), OrderStatus.PROCESSING));
    }

    @Test
    void requestReturnShouldThrowWhenNotOrderOwner() {
        Order order = Order.create(userWithProfile); // owned by "client"
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThrows(DomainException.class,
                () -> orderService.requestReturn(UUID.randomUUID(), "anotheruser"));
    }

    @Test
    void cancelOrderShouldThrowWhenNotOrderOwner() {
        Order order = Order.create(userWithProfile);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThrows(DomainException.class,
                () -> orderService.cancelOrder(UUID.randomUUID(), "anotheruser"));
    }

    @Test
    void cancelOrderShouldSyncBooksAndNotify() {
        Order order = Order.create(userWithProfile);
        order.addItem(book, 1);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.cancelOrder(UUID.randomUUID(), "client");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(searchSyncService, atLeastOnce()).syncBook(any(Book.class));
        verify(orderEmailService).sendStatusChanged(eq(order), eq(OrderStatus.CANCELLED));
    }

    @Test
    void approveReturnShouldSyncBooksAndNotify() {
        Order order = Order.create(userWithProfile);
        order.addItem(book, 2);
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        order.requestReturn();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.approveReturn(UUID.randomUUID());

        assertEquals(OrderStatus.RETURNED, order.getStatus());
        verify(searchSyncService, atLeastOnce()).syncBook(any(Book.class));
        verify(orderEmailService).sendStatusChanged(eq(order), eq(OrderStatus.RETURNED));
    }

    @Test
    void rejectReturnShouldRestoreDeliveredAndNotify() {
        Order order = Order.create(userWithProfile);
        order.updateStatus(OrderStatus.PROCESSING);
        order.updateStatus(OrderStatus.SHIPPED);
        order.updateStatus(OrderStatus.DELIVERED);
        order.requestReturn();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.rejectReturn(UUID.randomUUID());

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderEmailService).sendStatusChanged(eq(order), eq(OrderStatus.DELIVERED));
    }

    @Test
    void getOrderByIdShouldThrowForNonOwnerClient() {
        User otherUser = User.register("other", "o@test.com", "h", Role.CLIENT, null, null, null);
        Order order = Order.create(userWithProfile);
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        assertThrows(DomainException.class, () -> orderService.getOrderById(orderId, "other"));
    }

    @Test
    void getOrderByIdShouldAllowManagerToViewAnyOrder() {
        User manager = User.register("manager1", "m@test.com", "h", Role.MANAGER, null, null, null);
        Order order = Order.create(userWithProfile);
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));

        assertDoesNotThrow(() -> orderService.getOrderById(orderId, "manager1"));
    }
}
