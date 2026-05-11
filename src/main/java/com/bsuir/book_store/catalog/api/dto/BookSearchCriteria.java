package com.bsuir.book_store.catalog.api.dto;

import com.bsuir.book_store.catalog.domain.model.AgeRating;
import com.bsuir.book_store.catalog.domain.model.BookFormat;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^[a-z]{2}$", message = "language должен быть кодом ISO 639-1 из двух строчных букв (например, 'ru', 'en')")
    @Schema(description = "Язык издания (код языка, например 'ru', 'en')", example = "")
    private String language;

    @Schema(description = "Формат книги (HARDCOVER, SOFTCOVER)", example = "")
    private BookFormat format;

    @Schema(description = "Возрастной рейтинг", example = "")
    private AgeRating ageRating;

    @Schema(description = "Минимальный год публикации", example = "")
    private Integer minYear;

    @Schema(description = "Максимальный год публикации", example = "")
    private Integer maxYear;

    @Schema(description = "Минимальный средний рейтинг (0.0 - 5.0)", example = "")
    private Double minRating;

    @Schema(description = "Фильтр по удалённым книгам (только для менеджера). true — только удалённые, false/null — только активные", example = "")
    private Boolean deleted;
}