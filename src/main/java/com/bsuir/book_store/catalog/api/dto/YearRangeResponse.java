package com.bsuir.book_store.catalog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class YearRangeResponse {
    private Integer min;
    private Integer max;
}
