package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на импорт книги из Google Books")
public class ImportBookRequest {
    @Schema(description = "ISBN номер", example = "")
    private String isbn;

    @Schema(description = "Базовая цена")
    private BigDecimal price;

    @Schema(description = "Начальный остаток")
    private int stock;
}