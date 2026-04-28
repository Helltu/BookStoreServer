package com.bsuir.book_store.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на генерацию описания книги")
public class GenerateDescriptionRequest {
    @Schema(description = "Название книги", example = "Мастер и Маргарита")
    private String title;

    @Schema(description = "Авторы через запятую", example = "Михаил Булгаков")
    private String authors;

    @Schema(description = "Жанры через запятую", example = "Фантастика, Сатира")
    private String genres;
}
