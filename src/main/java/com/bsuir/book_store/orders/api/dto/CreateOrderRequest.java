package com.bsuir.book_store.orders.api.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    private List<ItemDto> items;
    private DeliveryDto deliveryDetails;

    @Data
    public static class ItemDto {
        private UUID bookId;
        private int quantity;
    }

    @Data
    public static class DeliveryDto {
        private String customerName;
        private String phone;
        private String address;
        private String timeSlot;
    }
}