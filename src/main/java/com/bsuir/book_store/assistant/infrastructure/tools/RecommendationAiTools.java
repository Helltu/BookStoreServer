package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.recommendations.application.RecommendationService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationAiTools {

    private final RecommendationService recommendationService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Tool("""
          Получить персональные рекомендации для ТЕКУЩЕГО пользователя на основе его истории заказов и вишлиста.
          Используй когда пользователь говорит: "что мне почитать", "порекомендуй что-нибудь", "что посоветуешь",
          "удиви меня" — БЕЗ конкретных критериев поиска.
          Возвращает готовый список — не нужно дополнительно искать через searchBooks.
          """)
    public String getPersonalRecommendations(@P("Количество книг (1-10). Если не указано — 5.") String limit) {
        log.info("AI tool [getPersonalRecommendations] limit={}", limit);
        try {
            int n = parseLimit(limit, 5);
            String username = currentUsername();
            List<BookDocument> books = recommendationService.personalRecommendations(username, n);
            if (books.isEmpty()) {
                return "Не удалось подобрать персональные рекомендации (нет истории заказов и вишлиста). Используй обычный поиск.";
            }
            return format(books);
        } catch (Exception e) {
            return "Не удалось получить рекомендации: " + e.getMessage();
        }
    }

    @Tool("""
          Получить книги, ПОХОЖИЕ на конкретную книгу (по смыслу описания).
          Используй когда пользователь говорит: "похоже на эту", "что-то такое же", "ещё в таком же стиле"
          и указывает конкретную книгу или её ID.
          """)
    public String getSimilarBooks(
            @P("UUID книги, к которой искать похожие") String bookId,
            @P("Количество (1-10). Если не указано — 5.") String limit
    ) {
        log.info("AI tool [getSimilarBooks] bookId={} limit={}", bookId, limit);
        try {
            int n = parseLimit(limit, 5);
            List<BookDocument> books = recommendationService.similarBooks(UUID.fromString(bookId), n);
            if (books.isEmpty()) return "Похожих книг не найдено.";
            return format(books);
        } catch (IllegalArgumentException e) {
            return "Некорректный ID книги.";
        } catch (Exception e) {
            return "Не удалось получить похожие: " + e.getMessage();
        }
    }

    @Tool("""
          Получить книги, которые часто покупают ВМЕСТЕ с указанной (на основе истории заказов всех клиентов).
          Используй когда пользователь говорит: "что с этим обычно берут", "сопутствующие", "что ещё покупают".
          """)
    public String getFrequentlyBoughtWith(
            @P("UUID книги-якоря") String bookId,
            @P("Количество (1-10). Если не указано — 5.") String limit
    ) {
        log.info("AI tool [getFrequentlyBoughtWith] bookId={} limit={}", bookId, limit);
        try {
            int n = parseLimit(limit, 5);
            List<BookDocument> books = recommendationService.frequentlyBoughtWith(UUID.fromString(bookId), n);
            if (books.isEmpty()) return "Данных о совместных покупках пока недостаточно.";
            return format(books);
        } catch (IllegalArgumentException e) {
            return "Некорректный ID книги.";
        } catch (Exception e) {
            return "Не удалось получить сопутствующие: " + e.getMessage();
        }
    }

    private String format(List<BookDocument> books) {
        return books.stream().map(b -> {
            String authors = b.getAuthors() == null ? "" : b.getAuthors().stream()
                    .map(a -> String.format("[%s](%s/author/%s)", a, frontendUrl,
                            URLEncoder.encode(a, StandardCharsets.UTF_8).replace("+", "%20")))
                    .collect(Collectors.joining(", "));
            String genres = b.getGenres() == null ? "" : b.getGenres().stream()
                    .map(g -> String.format("[%s](%s/genre/%s)", g, frontendUrl,
                            URLEncoder.encode(g, StandardCharsets.UTF_8).replace("+", "%20")))
                    .collect(Collectors.joining(", "));
            String rating = (b.getAverageRating() != null && b.getTotalReviews() != null)
                    ? String.format("%.1f/5 (%d отзывов)", b.getAverageRating(), b.getTotalReviews())
                    : "нет оценок";
            return String.format("bookUrl: %s/book/%s | Название: %s | Автор: %s | Жанры: %s | Цена: %s BYN | Рейтинг: %s",
                    frontendUrl, b.getId(), b.getTitle(), authors, genres, b.getPrice(), rating);
        }).collect(Collectors.joining("\n---\n"));
    }

    private int parseLimit(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Math.max(1, Math.min(10, Integer.parseInt(s.trim()))); }
        catch (NumberFormatException e) { return def; }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
