package com.bsuir.book_store.analytics.api;

import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse;
import com.bsuir.book_store.analytics.application.AnalyticsService;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Статистика и аналитика продаж")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Получить дашборд",
            description = "Возвращает сводные данные по продажам, топ книг и графики. Доступно только для роли MANAGER."
    )
    @GetMapping
    @IsManager
    public ResponseEntity<AnalyticsResponse> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardData());
    }
}