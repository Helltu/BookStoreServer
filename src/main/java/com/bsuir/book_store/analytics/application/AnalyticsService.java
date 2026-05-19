package com.bsuir.book_store.analytics.application;

import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse;
import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse.CustomerMetrics;
import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse.CustomerMetrics.TopCustomer;
import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse.ReturnMetrics;
import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse.StockMetrics;
import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse.StockMetrics.LowStockBook;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderItem;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<OrderStatus> EXCLUDED = List.of(OrderStatus.CANCELLED);
    private static final int TOP_CUSTOMERS_LIMIT = 5;
    private static final int TOP_BOOKS_LIMIT = 5;
    private static final int SLOW_MOVING_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getDashboardData(LocalDate startDate, LocalDate endDate, int lowStockThreshold) {
        Timestamp tsFrom = startDate != null ? Timestamp.valueOf(startDate.atStartOfDay()) : null;
        Timestamp tsTo = endDate != null ? Timestamp.valueOf(endDate.atTime(23, 59, 59)) : null;

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .filter(o -> {
                    LocalDate orderDate = o.getCreatedAt().toLocalDateTime().toLocalDate();
                    boolean afterStart = (startDate == null) || !orderDate.isBefore(startDate);
                    boolean beforeEnd = (endDate == null) || !orderDate.isAfter(endDate);
                    return afterStart && beforeEnd;
                })
                .toList();

        long totalOrders = orders.size();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalBooksSold = orders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();
        double averageCheck = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        Double avgDeliveryHoursRaw = orderRepository.avgDeliveryHours(OrderStatus.DELIVERED, tsFrom, tsTo);
        double avgDeliveryHours = avgDeliveryHoursRaw != null ? Math.round(avgDeliveryHoursRaw * 10.0) / 10.0 : 0.0;

        AnalyticsResponse.PeriodComparison periodComparison = null;
        if (startDate != null && endDate != null) {
            long periodDays = endDate.toEpochDay() - startDate.toEpochDay() + 1;
            LocalDate prevStart = startDate.minusDays(periodDays);
            LocalDate prevEnd = startDate.minusDays(1);
            Timestamp prevTsFrom = Timestamp.valueOf(prevStart.atStartOfDay());
            Timestamp prevTsTo = Timestamp.valueOf(prevEnd.atTime(23, 59, 59));

            List<Order> prevOrders = orderRepository.findAll().stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .filter(o -> {
                        LocalDate d = o.getCreatedAt().toLocalDateTime().toLocalDate();
                        return !d.isBefore(prevStart) && !d.isAfter(prevEnd);
                    })
                    .toList();

            long prevTotal = prevOrders.size();
            BigDecimal prevRevenue = prevOrders.stream().map(Order::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            long prevBooksSold = prevOrders.stream().flatMap(o -> o.getOrderItems().stream()).mapToLong(OrderItem::getQuantity).sum();
            double prevAvgCheck = prevTotal > 0
                    ? prevRevenue.divide(BigDecimal.valueOf(prevTotal), 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            periodComparison = AnalyticsResponse.PeriodComparison.builder()
                    .totalOrdersDelta(calcDelta(totalOrders, prevTotal))
                    .totalRevenueDelta(calcDelta(totalRevenue.doubleValue(), prevRevenue.doubleValue()))
                    .totalBooksSoldDelta(calcDelta(totalBooksSold, prevBooksSold))
                    .averageCheckDelta(calcDelta(averageCheck, prevAvgCheck))
                    .build();
        }

        Map<LocalDate, BigDecimal> revenueByDate = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDateTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalCost, BigDecimal::add)
                ));
        List<AnalyticsResponse.DatePoint> salesOverTime = revenueByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> AnalyticsResponse.DatePoint.builder()
                        .date(e.getKey().format(DateTimeFormatter.ISO_DATE))
                        .value(e.getValue())
                        .build())
                .toList();

        List<OrderItem> allItems = orders.stream().flatMap(o -> o.getOrderItems().stream()).toList();
        Map<String, Long> categoryStats = allItems.stream()
                .flatMap(item -> item.getBook().getGenres().stream())
                .collect(Collectors.groupingBy(g -> g.getName(), Collectors.counting()));
        List<AnalyticsResponse.CategoryPoint> salesByCategory = categoryStats.entrySet().stream()
                .map(e -> AnalyticsResponse.CategoryPoint.builder().category(e.getKey()).count(e.getValue()).build())
                .toList();

        Map<String, Long> bookStats = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getBookTitle, Collectors.summingLong(OrderItem::getQuantity)));
        List<AnalyticsResponse.BookPoint> topSellingBooks = bookStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_BOOKS_LIMIT)
                .map(e -> AnalyticsResponse.BookPoint.builder().title(e.getKey()).soldCount(e.getValue()).build())
                .toList();

        java.util.Set<String> topTitles = topSellingBooks.stream()
                .map(AnalyticsResponse.BookPoint::getTitle)
                .collect(Collectors.toSet());

        // Books with zero sales must appear first — fetch all from catalog, merge with bookStats
        List<AnalyticsResponse.BookPoint> slowMovingBooks = bookRepository.findAll().stream()
                .filter(b -> !topTitles.contains(b.getTitle()))
                .map(b -> AnalyticsResponse.BookPoint.builder()
                        .title(b.getTitle())
                        .soldCount(bookStats.getOrDefault(b.getTitle(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(AnalyticsResponse.BookPoint::getSoldCount))
                .limit(SLOW_MOVING_LIMIT)
                .toList();

        Map<String, Long> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        long uniqueCustomers = orderRepository.countUniqueCustomers(EXCLUDED, tsFrom, tsTo);
        List<Object[]> topCustomersRaw = orderRepository.findTopCustomers(EXCLUDED, tsFrom, tsTo, PageRequest.of(0, TOP_CUSTOMERS_LIMIT));
        List<TopCustomer> topCustomers = topCustomersRaw.stream()
                .map(row -> TopCustomer.builder()
                        .username((String) row[0])
                        .fullName(row[1] + " " + row[2])
                        .ordersCount(((Number) row[3]).longValue())
                        .totalSpent((BigDecimal) row[4])
                        .build())
                .toList();

        long returnRequested = orders.stream().filter(o -> o.getStatus() == OrderStatus.RETURN_REQUESTED).count();
        long returned = orders.stream().filter(o -> o.getStatus() == OrderStatus.RETURNED).count();
        BigDecimal returnedRevenue = orderRepository.sumTotalCostByStatus(OrderStatus.RETURNED);
        if (returnedRevenue == null) returnedRevenue = BigDecimal.ZERO;

        long outOfStockCount = bookRepository.countOutOfStock();
        List<LowStockBook> lowStockBooks = bookRepository
                .findByStockQuantityLessThanEqualAndDeletedAtIsNullOrderByStockQuantityAsc(lowStockThreshold)
                .stream()
                .map(b -> LowStockBook.builder().title(b.getTitle()).stockQuantity(b.getStockQuantity()).build())
                .toList();

        return AnalyticsResponse.builder()
                .summary(AnalyticsResponse.Summary.builder()
                        .totalOrders(totalOrders)
                        .totalRevenue(totalRevenue)
                        .totalBooksSold(totalBooksSold)
                        .averageCheck(averageCheck)
                        .avgDeliveryHours(avgDeliveryHours)
                        .build())
                .periodComparison(periodComparison)
                .salesOverTime(salesOverTime)
                .salesByCategory(salesByCategory)
                .topSellingBooks(topSellingBooks)
                .slowMovingBooks(slowMovingBooks)
                .ordersByStatus(ordersByStatus)
                .customerMetrics(CustomerMetrics.builder()
                        .uniqueCustomers(uniqueCustomers)
                        .topCustomers(topCustomers)
                        .build())
                .returnMetrics(ReturnMetrics.builder()
                        .returnRequested(returnRequested)
                        .returned(returned)
                        .returnedRevenue(returnedRevenue)
                        .build())
                .stockMetrics(StockMetrics.builder()
                        .outOfStockCount(outOfStockCount)
                        .lowStockBooks(lowStockBooks)
                        .build())
                .build();
    }

    private Double calcDelta(double current, double previous) {
        if (previous == 0) return null;
        return Math.round(((current - previous) / previous * 100) * 10.0) / 10.0;
    }
}
