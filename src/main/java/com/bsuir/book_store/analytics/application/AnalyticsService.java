package com.bsuir.book_store.analytics.application;

import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.domain.OrderItem;
import com.bsuir.book_store.orders.domain.OrderStatus;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getDashboardData() {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        long totalOrders = orders.size();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalCost) // В вашей сущности Order поле называется totalCost
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalBooksSold = orders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        double averageCheck = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // 3. График продаж по датам (Sales Over Time)
        Map<LocalDate, BigDecimal> revenueByDate = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDateTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalCost, BigDecimal::add)
                ));

        List<AnalyticsResponse.DatePoint> salesOverTime = revenueByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> AnalyticsResponse.DatePoint.builder()
                        .date(entry.getKey().format(DateTimeFormatter.ISO_DATE))
                        .value(entry.getValue())
                        .build())
                .toList();

        List<OrderItem> allItems = orders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .toList();

        Map<String, Long> categoryStats = allItems.stream()
                .flatMap(item -> item.getBook().getGenres().stream())
                .collect(Collectors.groupingBy(
                        genre -> genre.getName(),
                        Collectors.counting()
                ));

        List<AnalyticsResponse.CategoryPoint> salesByCategory = categoryStats.entrySet().stream()
                .map(entry -> AnalyticsResponse.CategoryPoint.builder()
                        .category(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();

        // 5. Топ продаваемых книг
        Map<String, Long> bookStats = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getBookTitle(), // Используем сохраненное название
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        List<AnalyticsResponse.BookPoint> topBooks = bookStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5) // Топ-5
                .map(entry -> AnalyticsResponse.BookPoint.builder()
                        .title(entry.getKey())
                        .soldCount(entry.getValue())
                        .build())
                .toList();

        return AnalyticsResponse.builder()
                .summary(AnalyticsResponse.Summary.builder()
                        .totalOrders(totalOrders)
                        .totalRevenue(totalRevenue)
                        .totalBooksSold(totalBooksSold)
                        .averageCheck(averageCheck)
                        .build())
                .salesOverTime(salesOverTime)
                .salesByCategory(salesByCategory)
                .topSellingBooks(topBooks)
                .build();
    }
}