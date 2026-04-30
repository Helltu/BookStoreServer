package com.bsuir.book_store.orders.api.dto.export;

import com.bsuir.book_store.orders.domain.OrderItem;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
public class OrderItemExportDto {
    UUID bookId;
    String bookTitle;
    String isbn;
    int quantity;
    BigDecimal pricePerItem;
    BigDecimal totalPrice;

    public static OrderItemExportDto from(OrderItem item) {
        return new OrderItemExportDto(
                item.getBook() != null ? item.getBook().getId() : null,
                item.getBookTitle(),
                item.getIsbn(),
                item.getQuantity(),
                item.getPricePerItem(),
                item.getTotalPrice()
        );
    }
}
