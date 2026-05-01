package com.bsuir.book_store.orders.application;

import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.api.dto.OrderAnalyticsResponse;
import com.bsuir.book_store.orders.api.dto.export.OrderExportDto;
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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SearchSyncService searchSyncService;
    private final OrderEmailService orderEmailService;

    @Transactional
    public UUID createOrder(CreateOrderRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        if (user.getFirstName() == null || user.getFirstName().isBlank() ||
            user.getLastName() == null || user.getLastName().isBlank()) {
            throw new DomainException("Для оформления заказа необходимо заполнить имя и фамилию в профиле");
        }

        Order order = Order.create(user);

        List<Book> reservedBooks = new ArrayList<>();
        for (var itemRequest : request.getItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId())
                    .orElseThrow(() -> new DomainException("Book not found: " + itemRequest.getBookId()));

            if (book.getStockQuantity() < itemRequest.getQuantity()) {
                throw new DomainException("Недостаточно товара '" + book.getTitle() + "' на складе. Доступно: " + book.getStockQuantity());
            }

            order.addItem(book, itemRequest.getQuantity());
            reservedBooks.add(book);
        }

        order.arrangeDelivery(
                request.getDeliveryDetails().getCustomerName(),
                request.getDeliveryDetails().getPhone(),
                request.getDeliveryDetails().getAddress(),
                request.getDeliveryDetails().getTimeSlot()
        );

        Order savedOrder = orderRepository.save(order);
        reservedBooks.forEach(searchSyncService::syncBook);
        orderEmailService.sendOrderCreated(savedOrder);
        return savedOrder.getId();
    }

    @Transactional
    public void changeStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Order not found"));

        boolean becomingCancelled = newStatus == OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.CANCELLED;

        order.updateStatus(newStatus);

        if (becomingCancelled) {
            order.getOrderItems().forEach(item -> searchSyncService.syncBook(item.getBook()));
        }

        orderEmailService.sendStatusChanged(order, newStatus);
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
    public Order getOrderById(UUID orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));

        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        boolean isManager = requester.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGER"));

        if (!isManager && !order.getUser().getUsername().equals(username)) {
            throw new DomainException("У вас нет прав на просмотр этого заказа");
        }

        order.getOrderItems().size();
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersFiltered(OrderStatus status, String orderNumber, String customerName,
                                          LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Timestamp tsFrom = from != null ? Timestamp.valueOf(from) : null;
        Timestamp tsTo = to != null ? Timestamp.valueOf(to) : null;
        Page<Order> orders = orderRepository.findWithFilters(status, orderNumber, customerName, tsFrom, tsTo, pageable);
        orders.forEach(o -> o.getOrderItems().size());
        return orders;
    }

    @Transactional(readOnly = true)
    public OrderAnalyticsResponse getAnalytics() {
        long total = orderRepository.count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), orderRepository.countByStatus(s));
        }

        BigDecimal revenueDelivered = orderRepository.sumTotalCostByStatus(OrderStatus.DELIVERED);
        if (revenueDelivered == null) revenueDelivered = BigDecimal.ZERO;

        BigDecimal revenueActive = Arrays.stream(OrderStatus.values())
                .filter(s -> s != OrderStatus.CANCELLED)
                .map(s -> {
                    BigDecimal sum = orderRepository.sumTotalCostByStatus(s);
                    return sum != null ? sum : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderAnalyticsResponse(total, byStatus, revenueDelivered, revenueActive);
    }

    @Transactional
    public void requestReturn(UUID orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new DomainException("У вас нет прав на управление этим заказом");
        }

        order.requestReturn();
        orderEmailService.sendStatusChanged(order, OrderStatus.RETURN_REQUESTED);
    }

    @Transactional
    public void approveReturn(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));

        order.approveReturn();
        order.getOrderItems().forEach(item -> searchSyncService.syncBook(item.getBook()));
        orderEmailService.sendStatusChanged(order, OrderStatus.RETURNED);
    }

    @Transactional
    public void rejectReturn(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));

        order.rejectReturn();
        orderEmailService.sendStatusChanged(order, OrderStatus.DELIVERED);
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
        orderEmailService.sendStatusChanged(order, OrderStatus.CANCELLED);
    }

    @Transactional
    public void updateDeliverySlot(UUID orderId, String timeSlot, java.time.LocalDate deliveryDate) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException("Заказ не найден"));
        DeliveryDetails details = order.getDeliveryDetails();
        if (details == null) {
            throw new DomainException("У заказа нет данных доставки");
        }
        details.setDeliveryTimeSlot(timeSlot);
        details.setDeliveryDate(java.sql.Date.valueOf(deliveryDate));
        orderEmailService.sendDeliverySlotAssigned(order);
    }

    @Transactional(readOnly = true)
    public List<OrderExportDto> exportOrders() {
        List<Order> orders = orderRepository.findAll();
        orders.forEach(o -> o.getOrderItems().size());
        return orders.stream().map(OrderExportDto::from).toList();
    }
}