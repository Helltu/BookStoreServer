package com.bsuir.book_store.analytics.api;

import com.bsuir.book_store.analytics.api.dto.AnalyticsResponse;
import com.bsuir.book_store.analytics.application.AnalyticsService;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @IsManager
    public ResponseEntity<AnalyticsResponse> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardData());
    }
}