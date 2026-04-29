package com.bsuir.book_store.orders.api.dto;

import com.bsuir.book_store.orders.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на обновление статуса заказа")
public class UpdateOrderStatusRequest {
    @NotNull(message = "Статус обязателен")
    @Schema(description = "Новый статус", example = "PROCESSING")
    private OrderStatus status;
}