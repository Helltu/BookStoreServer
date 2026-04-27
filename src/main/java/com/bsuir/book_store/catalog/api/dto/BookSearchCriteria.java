package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class BookSearchCriteria {
    @Schema(description = "Текстовый запрос (поиск по всем полям)", example = "")
    private String query;

    @Schema(description = "Минимальная цена", example = "")
    private BigDecimal minPrice;

    @Schema(description = "Максимальная цена", example = "")
    private BigDecimal maxPrice;

    @Schema(description = "Список жанров", example = "[]")
    private List<String> genres;

    @Schema(description = "Список авторов", example = "[]")
    private List<String> authors;

    @Schema(description = "Издательство", example = "")
    private String publisher;

    @Schema(description = "Только книги в наличии", example = "true")
    private Boolean inStock;
}