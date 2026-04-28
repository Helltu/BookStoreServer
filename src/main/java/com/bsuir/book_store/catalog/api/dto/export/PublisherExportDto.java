package com.bsuir.book_store.catalog.api.dto.export;

import java.util.UUID;

public record PublisherExportDto(
        UUID id,
        String name,
        String description,
        String logoUrl
) {}
