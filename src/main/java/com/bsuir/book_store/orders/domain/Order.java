package com.bsuir.book_store.orders.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryDetails deliveryDetails;

    @Column(name = "order_date")
    private Timestamp orderDate;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public static Order create(User user) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.user = user;
        order.status = OrderStatus.NEW;
        order.orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.totalCost = BigDecimal.ZERO;
        order.orderItems = new ArrayList<>();
        order.orderDate = Timestamp.valueOf(LocalDateTime.now());
        return order;
    }

    public void addItem(Book book, int quantity) {
        if (quantity <= 0) {
            throw new DomainException("Quantity must be greater than zero");
        }

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(this)
                .book(book)
                .bookTitle(book.getTitle())
                .isbn(book.getIsbn())
                .quantity(quantity)
                .pricePerItem(book.getCost())
                .build();

        this.orderItems.add(item);
        recalculateTotal();
    }

    public void attachDeliveryDetails(DeliveryDetails details) {
        if (details == null) throw new DomainException("Delivery details cannot be empty");
        this.deliveryDetails = details;
        details.setOrder(this); // Связываем двустороннюю связь
    }

    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.DELIVERED && newStatus == OrderStatus.CANCELLED) {
            throw new DomainException("Cannot cancel an already delivered order");
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new DomainException("Cannot change status of a cancelled order");
        }
        this.status = newStatus;
    }

    private void recalculateTotal() {
        this.totalCost = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}