package com.bsuir.book_store.orders.application;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID createOrder(CreateOrderRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        Order order = Order.create(user);

        for (var itemRequest : request.getItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId())
                    .orElseThrow(() -> new DomainException("Book not found: " + itemRequest.getBookId()));

            book.reserveStock(itemRequest.getQuantity());

            order.addItem(book, itemRequest.getQuantity());

            bookRepository.save(book);
        }

        DeliveryDetails delivery = DeliveryDetails.builder()
                .customerName(request.getDeliveryDetails().getCustomerName())
                .contactPhone(request.getDeliveryDetails().getPhone())
                .addressText(request.getDeliveryDetails().getAddress())
                .deliveryTimeSlot(request.getDeliveryDetails().getTimeSlot())
                .deliveryDate(Date.valueOf(LocalDate.now().plusDays(1)))
                .build();

        order.attachDeliveryDetails(delivery);

        return orderRepository.save(order).getId();
    }

    @Transactional
    public void changeStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Order not found"));

        order.updateStatus(newStatus);

        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrders(String username) {
        return orderRepository.findByUserUsernameOrderByCreatedAtDesc(username);
    }
}