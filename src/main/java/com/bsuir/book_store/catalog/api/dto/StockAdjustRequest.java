package com.bsuir.book_store.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StockAdjustRequest {
    @Schema(description = "Новое количество на складе", example = "50")
    private int quantity;
}
