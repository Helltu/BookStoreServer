package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.catalog.application.CatalogQueryService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogAiTools {

    private final CatalogQueryService catalogQueryService;

    // Паттерн для поиска специфических белорусских букв (ў, і, апостроф в контексте)
    private static final Pattern BELARUSIAN_PATTERN = Pattern.compile("[ўЎіІ']");
    // Паттерн для поиска кириллицы в целом
    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("\\p{IsCyrillic}");

    @Tool("""
          Инструмент поиска книг. 
          Ассистент должен сам решать, на каком языке искать, исходя из контекста диалога.
          
          РЕКОМЕНДАЦИИ ПО ИСПОЛЬЗОВАНИЮ:
          1. Если пользователь ищет конкретную книгу на русском/белорусском -> передавай название как есть.
          2. Если пользователь ищет IT-литературу или международный бестселлер -> попробуй найти оригинал на английском ИЛИ перевод.
          3. Если результатов мало -> попробуй перефразировать запрос или перевести ключевые слова.
          """)
    public String searchBooks(String query) {
        log.info("AI tool [searchBooks] called with query: '{}'", query);

        List<BookDocument> results = catalogQueryService.search(query);

        if (results.isEmpty()) {
            return String.format("По запросу '%s' ничего не найдено. Попробуй изменить язык запроса (например, с русского на английский) или упростить его.", query);
        }

        return results.stream()
                .limit(7)
                .map(b -> {
                    String language = detectRealLanguage(b); // Реальное определение языка
                    return String.format("ID: %s | Название: %s | Автор: %s | Язык издания: %s | Цена: %s BYN | Описание: %s",
                            b.getId(),
                            b.getTitle(),
                            String.join(", ", b.getAuthors()),
                            language,
                            b.getPrice(),
                            truncate(b.getDescription(), 120));
                })
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("""
          Получает полные детали книги по ID (предпочтительно) или названию.
          Вызывай этот инструмент, когда нужно дать развернутый ответ о конкретной книге.
          """)
    public String getBookDetails(String idOrTitle) {
        log.info("AI tool [getBookDetails] called with: '{}'", idOrTitle);

        List<BookDocument> results = catalogQueryService.search(idOrTitle);

        if (results.isEmpty()) {
            return "Книга не найдена. Проверь ID.";
        }

        BookDocument book = results.get(0);
        String language = detectRealLanguage(book);

        return String.format("""
                --- ПОДРОБНАЯ ИНФОРМАЦИЯ ---
                ID: %s
                Название: %s
                Автор(ы): %s
                Жанры: %s
                Язык издания: %s
                Цена: %s BYN
                
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
                book.getDescription()
        );
    }

    private String truncate(String text, int length) {
        if (text == null || text.isBlank()) return "Без описания";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }

    private String detectRealLanguage(BookDocument book) {
        // Собираем весь доступный текст для анализа
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