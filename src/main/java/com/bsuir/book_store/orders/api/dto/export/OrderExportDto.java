package com.bsuir.book_store.orders.api.dto.export;

import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderStatus;
import lombok.Value;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Value
public class OrderExportDto {
    UUID id;
    String orderNumber;
    String userEmail;
    OrderStatus status;
    BigDecimal totalCost;
    Timestamp orderDate;
    Timestamp createdAt;
    List<OrderItemExportDto> items;

    public static OrderExportDto from(Order order) {
        return new OrderExportDto(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getTotalCost(),
                order.getOrderDate(),
                order.getCreatedAt(),
                order.getOrderItems().stream().map(OrderItemExportDto::from).toList()
        );
    }
}
