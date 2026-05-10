package com.bsuir.book_store.catalog.domain.model;

public enum AgeRating {
    AGE_0_PLUS("0+"),
    AGE_6_PLUS("6+"),
    AGE_12_PLUS("12+"),
    AGE_16_PLUS("16+"),
    AGE_18_PLUS("18+");

    private final String label;

    AgeRating(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
