package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateBookRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private int stock;
    private List<UUID> authorIds;
    private List<UUID> genreIds;
    private UUID publisherId;
    private List<String> keywords;

    private MultipartFile coverFile;
    private List<MultipartFile> previewFiles;
}