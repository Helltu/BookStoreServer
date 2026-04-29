package com.bsuir.book_store.orders.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на оформление заказа")
public class CreateOrderRequest {
    @NotEmpty(message = "Список товаров не может быть пустым")
    @Valid
    @Schema(description = "Список товаров в корзине", example = "[{\"bookId\": \"\", \"quantity\": 1}]")
    private List<ItemDto> items;

    @NotNull(message = "Детали доставки обязательны")
    @Valid
    @Schema(description = "Детали доставки")
    private DeliveryDto deliveryDetails;

    @Data
    public static class ItemDto {
        @NotNull(message = "ID книги обязателен")
        @Schema(description = "ID книги", example = "")
        private UUID bookId;

        @Min(value = 1, message = "Количество должно быть не менее 1")
        @Schema(description = "Количество")
        private int quantity;
    }

    @Data
    public static class DeliveryDto {
        @NotBlank(message = "Имя получателя обязательно")
        @Schema(description = "Имя получателя", example = "")
        private String customerName;

        @NotBlank(message = "Телефон обязателен")
        @Schema(description = "Телефон", example = "")
        private String phone;

        @NotBlank(message = "Адрес обязателен")
        @Schema(description = "Адрес", example = "")
        private String address;

        @Schema(description = "Желаемое время", example = "")
        private String timeSlot;
    }
}