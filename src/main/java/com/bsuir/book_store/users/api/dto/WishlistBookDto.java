package com.bsuir.book_store.users.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Книга в списке желаемого")
public class WishlistBookDto {
    private UUID id;
    private String title;
    private BigDecimal cost;
    private String coverImageUrl;
}
