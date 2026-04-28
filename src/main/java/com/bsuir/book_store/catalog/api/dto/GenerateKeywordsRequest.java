package com.bsuir.book_store.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Запрос на генерацию ключевых слов")
public class GenerateKeywordsRequest {
    @Schema(description = "Название книги", example = "Мастер и Маргарита")
    private String title;

    @Schema(description = "Описание книги", example = "Роман о дьяволе, посетившем Москву...")
    private String description;

    @Schema(description = "Уже существующие ключевые слова")
    private Set<String> existingKeywords;
}
