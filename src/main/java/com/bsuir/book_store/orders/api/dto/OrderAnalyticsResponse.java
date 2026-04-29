package com.bsuir.book_store.orders.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@Schema(description = "Аналитика по заказам")
public class OrderAnalyticsResponse {
    @Schema(description = "Общее количество заказов")
    private long totalOrders;

    @Schema(description = "Количество заказов по статусам")
    private Map<String, Long> ordersByStatus;

    @Schema(description = "Выручка по доставленным заказам")
    private BigDecimal revenueDelivered;

    @Schema(description = "Выручка по всем активным заказам (не отменены)")
    private BigDecimal revenueActive;
}
