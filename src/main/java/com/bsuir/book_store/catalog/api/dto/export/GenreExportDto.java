package com.bsuir.book_store.catalog.api.dto.export;

import java.util.UUID;

public record GenreExportDto(
        UUID id,
        String name
) {}
