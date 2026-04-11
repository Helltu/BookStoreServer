package com.bsuir.book_store.catalog.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BookSearchCriteria {
    private String query;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> genres;
    private List<String> authors;
    private String publisher;
}