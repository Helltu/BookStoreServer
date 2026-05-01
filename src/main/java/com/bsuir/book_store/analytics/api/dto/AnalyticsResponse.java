package com.bsuir.book_store.analytics.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private Summary summary;
    private PeriodComparison periodComparison;
    private List<DatePoint> salesOverTime;
    private List<CategoryPoint> salesByCategory;
    private List<BookPoint> topSellingBooks;
    private List<BookPoint> slowMovingBooks;
    private CustomerMetrics customerMetrics;
    private ReturnMetrics returnMetrics;
    private StockMetrics stockMetrics;
    private Map<String, Long> ordersByStatus;

    @Data
    @Builder
    public static class Summary {
        private long totalOrders;
        private BigDecimal totalRevenue;
        private long totalBooksSold;
        private double averageCheck;
        private double avgDeliveryHours;
    }

    @Data
    @Builder
    public static class PeriodComparison {
        private Double totalOrdersDelta;
        private Double totalRevenueDelta;
        private Double totalBooksSoldDelta;
        private Double averageCheckDelta;
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

    @Data
    @Builder
    public static class CustomerMetrics {
        private long uniqueCustomers;
        private List<TopCustomer> topCustomers;

        @Data
        @Builder
        public static class TopCustomer {
            private String username;
            private String fullName;
            private long ordersCount;
            private BigDecimal totalSpent;
        }
    }

    @Data
    @Builder
    public static class ReturnMetrics {
        private long returnRequested;
        private long returned;
        private BigDecimal returnedRevenue;
    }

    @Data
    @Builder
    public static class StockMetrics {
        private long outOfStockCount;
        private List<LowStockBook> lowStockBooks;

        @Data
        @Builder
        public static class LowStockBook {
            private String title;
            private int stockQuantity;
        }
    }
}
