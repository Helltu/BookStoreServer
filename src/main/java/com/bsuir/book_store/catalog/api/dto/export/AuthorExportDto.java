package com.bsuir.book_store.catalog.api.dto.export;

import java.util.UUID;

public record AuthorExportDto(
        UUID id,
        String name,
        String biography,
        String photoUrl
) {}
