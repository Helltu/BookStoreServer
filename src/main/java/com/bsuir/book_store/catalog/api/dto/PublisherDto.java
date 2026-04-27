package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "Данные издательства")
public class PublisherDto {
    @Schema(description = "Название издательства", example = "")
    private String name;

    @Schema(description = "Описание (опционально)", example = "")
    private String description;

    private MultipartFile logo;
}