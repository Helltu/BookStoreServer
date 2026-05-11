package com.bsuir.book_store.recommendations.api;

import com.bsuir.book_store.recommendations.api.dto.RecommendedBookDto;
import com.bsuir.book_store.recommendations.application.CoOccurrenceService;
import com.bsuir.book_store.recommendations.application.RecommendationService;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Рекомендательная система: похожие книги, частые комплекты, персональная лента")
public class RecommendationController {

    private static final int DEFAULT_TOP_K = 8;
    private static final int MAX_TOP_K = 30;

    private final RecommendationService recommendationService;
    private final CoOccurrenceService coOccurrenceService;

    @Operation(summary = "Похожие книги (по смыслу описания)")
    @GetMapping("/similar/{bookId}")
    public ResponseEntity<List<RecommendedBookDto>> similar(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(recommendationService.similarBooks(bookId, clamp(limit)).stream()
                .map(RecommendedBookDto::from)
                .toList());
    }

    @Operation(summary = "С этой книгой часто покупают (collaborative)")
    @GetMapping("/frequently-bought-with/{bookId}")
    public ResponseEntity<List<RecommendedBookDto>> frequentlyBoughtWith(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(recommendationService.frequentlyBoughtWith(bookId, clamp(limit)).stream()
                .map(RecommendedBookDto::from)
                .toList());
    }

    @Operation(summary = "Персональные рекомендации для текущего пользователя")
    @GetMapping("/personal")
    public ResponseEntity<List<RecommendedBookDto>> personal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(recommendationService.personalRecommendations(userDetails.getUsername(), clamp(limit)).stream()
                .map(RecommendedBookDto::from)
                .toList());
    }

    @Operation(summary = "Принудительно пересчитать матрицу co-occurrence (только менеджер)")
    @PostMapping("/co-occurrence/recompute")
    @IsManager
    public ResponseEntity<Void> recomputeCoOccurrence() {
        coOccurrenceService.recompute();
        return ResponseEntity.noContent().build();
    }

    private int clamp(int v) {
        if (v <= 0) return DEFAULT_TOP_K;
        return Math.min(v, MAX_TOP_K);
    }
}
