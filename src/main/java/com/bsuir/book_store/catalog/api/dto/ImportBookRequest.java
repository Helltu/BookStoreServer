package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ImportBookRequest {
    private String isbn;
    private BigDecimal price;
    private int stock;
}