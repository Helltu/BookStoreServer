package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Данные жанра")
public class GenreDto {
    @Schema(description = "Название жанра", example = "")
    private String name;
}