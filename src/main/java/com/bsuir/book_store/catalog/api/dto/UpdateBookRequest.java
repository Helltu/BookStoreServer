package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на обновление книги (оставьте поля пустыми, если не хотите их менять)")
public class UpdateBookRequest {
    @Schema(description = "Название книги", example = "")
    private String title;

    @Schema(description = "Описание", example = "")
    private String description;

    @Schema(description = "Цена (BYN)")
    private BigDecimal price;

    @Schema(description = "Количество на складе")
    private Integer stock;

    @Schema(description = "ID авторов", example = "[]")
    private List<UUID> authorIds;

    @Schema(description = "ID жанров", example = "[]")
    private List<UUID> genreIds;

    @Schema(description = "ID издательства (опционально)", example = "")
    private UUID publisherId;

    @Schema(description = "Ключевые слова", example = "[]")
    private List<String> keywords;

    private MultipartFile coverFile;

    private List<MultipartFile> previewFiles;
}