package com.bsuir.book_store.catalog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PriceRangeResponse {
    private BigDecimal min;
    private BigDecimal max;
}
