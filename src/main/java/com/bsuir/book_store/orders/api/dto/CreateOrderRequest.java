package com.bsuir.book_store.orders.api.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на оформление заказа")
public class CreateOrderRequest {
    @Schema(description = "Список товаров в корзине", example = "[{\"bookId\": \"\", \"quantity\": 1}]")
    private List<ItemDto> items;
    
    @Schema(description = "Детали доставки")
    private DeliveryDto deliveryDetails;

    @Data
    public static class ItemDto {
        @Schema(description = "ID книги", example = "")
        private UUID bookId;
        
        @Schema(description = "Количество")
        private int quantity;
    }

    @Data
    public static class DeliveryDto {
        @Schema(description = "Имя получателя", example = "")
        private String customerName;
        @Schema(description = "Телефон", example = "")
        private String phone;
        @Schema(description = "Адрес", example = "")
        private String address;
        @Schema(description = "Желаемое время", example = "")
        private String timeSlot;
    }
}