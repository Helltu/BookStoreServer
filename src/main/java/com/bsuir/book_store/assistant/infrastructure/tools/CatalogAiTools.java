package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.assistant.infrastructure.cache.UserProfileCache;
import com.bsuir.book_store.catalog.api.dto.BookSearchCriteria;
import com.bsuir.book_store.catalog.application.CatalogQueryService;
import com.bsuir.book_store.catalog.application.SemanticSearchService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogAiTools {

    private final CatalogQueryService catalogQueryService;
    private final SemanticSearchService semanticSearchService;
    private final UserProfileCache profileCache;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final Pattern BELARUSIAN_PATTERN = Pattern.compile("[ўЎіІ']");
    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("\\p{IsCyrillic}");

    @Tool("""
          Структурный поиск книг по КОНКРЕТНЫМ критериям: название, автор, жанр, ценовой диапазон, язык.
          ⚠️ НЕ используй для абстрактных/эмоциональных запросов ("весёлое", "грустное", "атмосферное", "про одиночество") —
          для таких запросов есть отдельный инструмент `searchBooksSemantic`.
          Все параметры — строки. Пустая строка = критерий не указан.
          Примеры:
            "Толстой" -> query="Толстой"
            "фантастика до 30 рублей" -> query="фантастика", maxPrice="30"
            "зарубежные детективы" -> query="детектив", language="EN"
          """)
    public String searchBooks(
            @P("Текстовый запрос: ключевые слова, название, автор. Может быть пустой строкой если фильтруем только по жанру/цене.") String query,
            @P("Язык книги: RU (русский), BE (белорусский), EN (английский / зарубежные). Пустая строка если язык не важен.") String language,
            @P("Минимальная цена в BYN как число (например '20'). Пустая строка если не указано.") String minPrice,
            @P("Максимальная цена в BYN как число (например '50'). Пустая строка если не указано.") String maxPrice,
            @P("Список жанров через запятую (например: 'Фантастика, Детектив'). Пустая строка если не указано.") String genres
    ) {
        log.info("AI tool [searchBooks] query='{}', lang={}, price=[{}..{}], genres={}",
                query, language, minPrice, maxPrice, genres);

        BookSearchCriteria criteria = new BookSearchCriteria();
        criteria.setQuery(query == null ? "" : query);
        criteria.setMinPrice(parseDecimal(minPrice));
        criteria.setMaxPrice(parseDecimal(maxPrice));
        criteria.setInStock(true);
        if (genres != null && !genres.isBlank()) {
            criteria.setGenres(Arrays.stream(genres.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }

        Set<UUID> excluded = getExcludedBookIds();
        String langFilter = normalizeLanguage(language);

        List<BookDocument> results = catalogQueryService.search(criteria, PageRequest.of(0, 30), false).getContent()
                .stream()
                .filter(b -> !excluded.contains(toUuid(b.getId())))
                .filter(b -> langFilter == null || detectRealLanguage(b).startsWith(langFilter))
                .limit(7)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            return String.format("По запросу не найдено книг (с учётом фильтров: язык=%s, цена=[%s..%s], жанры=%s). Попробуй ослабить фильтры или изменить ключевые слова.",
                    language, minPrice, maxPrice, genres);
        }

        return formatResults(results);
    }

    private String formatResults(List<BookDocument> results) {
        return results.stream()
                .map(b -> {
                    String bookLang = detectRealLanguage(b);
                    String authorsStr = b.getAuthors().stream()
                            .map(a -> String.format("[%s](%s/author/%s)", a, frontendUrl, URLEncoder.encode(a, StandardCharsets.UTF_8).replace("+", "%20")))
                            .collect(Collectors.joining(", "));
                    String genresStr = b.getGenres().stream()
                            .map(g -> String.format("[%s](%s/genre/%s)", g, frontendUrl, URLEncoder.encode(g, StandardCharsets.UTF_8).replace("+", "%20")))
                            .collect(Collectors.joining(", "));
                    String rating = (b.getAverageRating() != null && b.getTotalReviews() != null)
                            ? String.format("%.1f/5 (%d отзывов)", b.getAverageRating(), b.getTotalReviews())
                            : "нет оценок";
                    return String.format("bookUrl: %s/book/%s | Название: %s | Автор: %s | Жанры: %s | Язык: %s | Цена: %s BYN | Рейтинг: %s | Описание: %s",
                            frontendUrl,
                            b.getId(),
                            b.getTitle(),
                            authorsStr,
                            genresStr,
                            bookLang,
                            b.getPrice(),
                            rating,
                            truncate(b.getDescription(), 120));
                })
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("""
          🎯 ОСНОВНОЙ инструмент для запросов про НАСТРОЕНИЕ / АТМОСФЕРУ / ЭМОЦИИ книги.
          Используй когда пользователь хочет:
            - "что-нибудь весёлое / смешное / лёгкое"
            - "что-то грустное / трогательное / меланхоличное"
            - "атмосферное / мрачное / уютное / страшное"
            - "после которой хочется плакать / задуматься"
            - "похоже на <название другой книги>"
            - "про одиночество / любовь / поиск себя" (без конкретного жанра)
          ⚠️ ВАЖНО: переводи запрос на АНГЛИЙСКИЙ — модель работает лучше на английском.
          Передавай 3-5 ключевых описательных слов на английском.
          Примеры:
            "весёлое" → "funny humorous lighthearted comedy"
            "грустное" → "sad melancholic emotional bittersweet"
            "атмосферное про город" → "atmospheric urban moody"
          """)
    public String searchBooksSemantic(@P("Натуральный запрос на естественном языке") String naturalQuery) {
        log.info("AI tool [searchBooksSemantic] query='{}'", naturalQuery);

        Set<UUID> excluded = getExcludedBookIds();
        List<BookDocument> results = semanticSearchService.semanticSearch(naturalQuery, 20)
                .stream()
                .filter(b -> b.getStockQuantity() != null && b.getStockQuantity() > 0)
                .filter(b -> !excluded.contains(toUuid(b.getId())))
                .limit(7)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            return "По смысловому поиску ничего подходящего не найдено. Попробуй переформулировать или используй обычный searchBooks.";
        }
        return formatResults(results);
    }

    @Tool("""
          Получает полные детали книги по ID (предпочтительно) или названию.
          Вызывай этот инструмент, когда нужно дать развернутый ответ о конкретной книге.
          """)
    public String getBookDetails(String idOrTitle) {
        log.info("AI tool [getBookDetails] called with: '{}'", idOrTitle);

        BookSearchCriteria criteria = new BookSearchCriteria();
        criteria.setQuery(idOrTitle);

        List<BookDocument> results = catalogQueryService.search(criteria, PageRequest.of(0, 1), false).getContent();

        if (results.isEmpty()) {
            return "Книга не найдена. Проверь ID.";
        }

        BookDocument book = results.get(0);
        String language = detectRealLanguage(book);

        String rating = (book.getAverageRating() != null && book.getTotalReviews() != null)
                ? String.format("%.1f/5 (%d отзывов)", book.getAverageRating(), book.getTotalReviews())
                : "нет оценок";
        return String.format("""
                --- ПОДРОБНАЯ ИНФОРМАЦИЯ ---
                ID: %s
                Название: %s
                Автор(ы): %s
                Жанры: %s
                Язык издания: %s
                Цена: %s BYN
                Рейтинг: %s

                Полная аннотация:
                %s
                ----------------------------
                """,
                book.getId(),
                book.getTitle(),
                String.join(", ", book.getAuthors()),
                String.join(", ", book.getGenres()),
                language,
                book.getPrice(),
                rating,
                book.getDescription()
        );
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) return null;
        String l = language.trim().toUpperCase();
        if (l.startsWith("RU") || l.contains("РУС")) return "RUSSIAN";
        if (l.startsWith("BE") || l.startsWith("BY") || l.contains("БЕЛ")) return "BELARUSIAN";
        if (l.startsWith("EN") || l.contains("АНГЛ") || l.contains("ЗАРУБЕЖ")) return "ENGLISH";
        return null;
    }

    private UUID toUuid(String id) {
        try { return UUID.fromString(id); } catch (Exception e) { return null; }
    }

    private Set<UUID> getExcludedBookIds() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return profileCache.get(username).getExcludedBookIds();
        } catch (Exception e) {
            return Set.of();
        }
    }

    private String truncate(String text, int length) {
        if (text == null || text.isBlank()) return "Без описания";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }

    private String detectRealLanguage(BookDocument book) {
        String textToAnalyze = (book.getTitle() + " " + book.getDescription()).toLowerCase();

        if (BELARUSIAN_PATTERN.matcher(textToAnalyze).find()) {
            return "BELARUSIAN (BY)";
        }

        if (CYRILLIC_PATTERN.matcher(textToAnalyze).find()) {
            return "RUSSIAN (RU)";
        }

        return "ENGLISH (EN)";
    }
}