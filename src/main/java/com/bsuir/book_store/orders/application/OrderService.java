package com.bsuir.book_store.orders.application;

import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.domain.DeliveryDetails;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SearchSyncService searchSyncService;

    @Transactional
    public UUID createOrder(CreateOrderRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        Order order = Order.create(user);

        List<Book> reservedBooks = new ArrayList<>();
        for (var itemRequest : request.getItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId())
                    .orElseThrow(() -> new DomainException("Book not found: " + itemRequest.getBookId()));

            order.addItem(book, itemRequest.getQuantity());
            reservedBooks.add(book);
        }

        order.arrangeDelivery(
                request.getDeliveryDetails().getCustomerName(),
                request.getDeliveryDetails().getPhone(),
                request.getDeliveryDetails().getAddress(),
                request.getDeliveryDetails().getTimeSlot()
        );

        UUID orderId = orderRepository.save(order).getId();
        reservedBooks.forEach(searchSyncService::syncBook);
        return orderId;
    }

    @Transactional
    public void changeStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Order not found"));

        boolean becomingCancelled = newStatus == OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.CANCELLED;

        order.updateStatus(newStatus);
        orderRepository.save(order);

        if (becomingCancelled) {
            order.getOrderItems().forEach(item -> searchSyncService.syncBook(item.getBook()));
        }
    }

    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        // Инициализация ленивых коллекций, чтобы избежать LazyInitializationException
        orders.forEach(o -> o.getOrderItems().size());
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrders(String username) {
        return orderRepository.findByUserUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));
        order.getOrderItems().size(); // Инициализация ленивой коллекции товаров
        return order;
    }

    @Transactional
    public void cancelOrder(UUID orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new DomainException("У вас нет прав на отмену этого заказа");
        }

        order.cancelByUser();
        order.getOrderItems().forEach(item -> searchSyncService.syncBook(item.getBook()));
    }
}