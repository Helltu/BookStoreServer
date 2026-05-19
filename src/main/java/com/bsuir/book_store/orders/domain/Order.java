package com.bsuir.book_store.orders.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private DeliveryDetails deliveryDetails;

    @Column(name = "order_date")
    private Timestamp orderDate;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public static Order create(User user) {
        Order order = new Order();
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
            throw new DomainException("Количество должно быть больше нуля");
        }
        
        book.reserveStock(quantity); // Вся магия теперь внутри!

        OrderItem item = OrderItem.builder()
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

    public void arrangeDelivery(String customerName, String phone, String addressText, String timeSlot) {
        DeliveryDetails details = DeliveryDetails.builder()
                .customerName(customerName)
                .contactPhone(phone)
                .addressText(addressText)
                .deliveryTimeSlot(null)
                .deliveryDate(null)
                .build();
        
        this.deliveryDetails = details;
        details.setOrder(this);
    }

    public void guardImmutable() {
        if (this.status == OrderStatus.CANCELLED || this.status == OrderStatus.RETURNED) {
            throw new DomainException("Заказ в статусе " + this.status + " не может быть изменён");
        }
    }

    public void updateStatus(OrderStatus newStatus) {
        guardImmutable();
        if (this.status == OrderStatus.RETURN_REQUESTED) {
            throw new DomainException("Для управления возвратом используйте соответствующие эндпоинты");
        }

        boolean validTransition = switch (this.status) {
            case NEW -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.FAILED;
            case FAILED -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!validTransition) {
            throw new DomainException(
                    "Недопустимый переход статуса: " + this.status + " → " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : this.orderItems) {
                item.getBook().releaseStock(item.getQuantity());
            }
        }

        this.status = newStatus;
    }

    private void recalculateTotal() {
        this.totalCost = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void shipOrder(String timeSlot, java.time.LocalDate deliveryDate) {
        guardImmutable();
        if (this.status != OrderStatus.PROCESSING && this.status != OrderStatus.FAILED) {
            throw new DomainException("Отправить можно только заказ в статусе PROCESSING или FAILED");
        }
        if (this.deliveryDetails == null) {
            throw new DomainException("У заказа нет данных доставки");
        }
        this.deliveryDetails.setDeliveryTimeSlot(timeSlot);
        this.deliveryDetails.setDeliveryDate(java.sql.Date.valueOf(deliveryDate));
        this.status = OrderStatus.SHIPPED;
    }

    public void cancelByUser() {
        if (this.status != OrderStatus.NEW) {
            throw new DomainException("Отменить заказ можно только пока он находится в статусе NEW");
        }
        this.updateStatus(OrderStatus.CANCELLED);
    }

    public void requestReturn() {
        guardImmutable();
        if (this.status != OrderStatus.DELIVERED) {
            throw new DomainException("Запросить возврат можно только для доставленных заказов");
        }
        this.status = OrderStatus.RETURN_REQUESTED;
    }

    public void approveReturn() {
        if (this.status != OrderStatus.RETURN_REQUESTED) {
            throw new DomainException("Подтвердить возврат можно только для заказов в статусе RETURN_REQUESTED");
        }
        for (OrderItem item : this.orderItems) {
            item.getBook().releaseStock(item.getQuantity());
        }
        this.status = OrderStatus.RETURNED;
    }

    public void rejectReturn() {
        if (this.status != OrderStatus.RETURN_REQUESTED) {
            throw new DomainException("Отклонить возврат можно только для заказов в статусе RETURN_REQUESTED");
        }
        this.status = OrderStatus.DELIVERED;
    }
}