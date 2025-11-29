package com.bsuir.book_store.analytics.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsResponse {
    private Summary summary;

    private List<DatePoint> salesOverTime;

    private List<CategoryPoint> salesByCategory;

    private List<BookPoint> topSellingBooks;

    @Data
    @Builder
    public static class Summary {
        private long totalOrders;
        private BigDecimal totalRevenue;
        private long totalBooksSold;
        private double averageCheck;
    }

    @Data
    @Builder
    public static class DatePoint {
        private String date;
        private BigDecimal value;
    }

    @Data
    @Builder
    public static class CategoryPoint {
        private String category;
        private long count;
    }

    @Data
    @Builder
    public static class BookPoint {
        private String title;
        private long soldCount;
    }
}