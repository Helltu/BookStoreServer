package com.bsuir.book_store.catalog.api.dto.export;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record BookExportDto(
        UUID id,
        String title,
        String isbn,
        String description,
        int stockQuantity,
        BigDecimal cost,
        UUID publisherId,
        List<UUID> authorIds,
        List<UUID> genreIds,
        Set<String> keywords
) {}
