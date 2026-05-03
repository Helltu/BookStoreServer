package com.bsuir.book_store.catalog.api.dto;

import com.bsuir.book_store.catalog.domain.model.BookFormat;
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

    @Schema(description = "Количество страниц")
    private Integer pagesCount;

    @Schema(description = "Формат издания")
    private BookFormat format;

    @Schema(description = "Вес книги в граммах")
    private Double weight;

    @Schema(description = "Размеры книги")
    private String dimensions;

    @Schema(description = "Возрастное ограничение")
    private String ageRating;

    @Schema(description = "Год издания")
    private Integer publicationYear;

    @Schema(description = "Язык издания")
    private String language;

    @Schema(description = "Язык оригинала")
    private String originalLanguage;

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

    @Schema(description = "URL превью которые нужно сохранить (null = не менять превью, [] = удалить все)")
    private List<String> keepPreviewUrls;
}
