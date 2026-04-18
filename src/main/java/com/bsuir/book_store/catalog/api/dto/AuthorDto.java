package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "Данные автора")
public class AuthorDto {
    @Schema(description = "Имя автора", example = "")
    private String name;

    @Schema(description = "Биография (опционально)", example = "")
    private String biography;

    @Schema(description = "Фотография автора (опционально)")
    private MultipartFile photo;
}