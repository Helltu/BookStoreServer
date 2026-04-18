package com.bsuir.book_store.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос с ключевым словом")
public class KeywordRequest {
    @Schema(description = "Ключевое слово", example = "")
    private String keyword;
}