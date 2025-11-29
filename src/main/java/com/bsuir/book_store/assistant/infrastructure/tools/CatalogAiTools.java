package com.bsuir.book_store.assistant.infrastructure.tools;

import com.bsuir.book_store.catalog.application.CatalogQueryService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CatalogAiTools {

    private final CatalogQueryService catalogQueryService;

    @Tool("""
          Основной инструмент для поиска книг. Используй его первым при любом запросе литературы.
          
          ВАЖНЫЕ ПРАВИЛА ВЫЗОВА:
          1. ПЕРЕВОД: База данных содержит книги ТОЛЬКО на английском языке.
             Если запрос пользователя на русском (например: "книги по многопоточности в Java"),
             ты ОБЯЗАН перевести суть в английские ключевые слова (например: "Java Concurrency" или "Multithreading").
          2. ОЧИСТКА: Убирай слова-паразиты ("посоветуй", "какие есть", "хочу почитать").
             Оставляй только техническую суть: технологии, языки, паттерны, авторов.
          3. КОНТЕКСТ: Если пользователь ищет "для начинающих", добавь в запрос слова "Beginner", "Intro", "Head First".
          """)
    public String searchBooks(String query) {
        System.out.println("DEBUG: AI tool call searchBooks with query: '" + query + "'");

        List<BookDocument> results = catalogQueryService.search(query);

        if (results.isEmpty()) {
            return "По запросу '" + query + "' ничего не найдено.";
        }

        return results.stream()
                .limit(5)
                .map(b -> String.format("ID: %s | Название: %s | Автор: %s | Цена: %s BYN | Описание: %s",
                        b.getId(),
                        b.getTitle(),
                        String.join(", ", b.getAuthors()),
                        b.getPrice(),
                        truncate(b.getDescription(), 150)))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("""
          Используй этот инструмент, когда пользователь заинтересовался КОНКРЕТНОЙ книгой из списка поиска
          и просит рассказать о ней подробнее (содержание, полная аннотация, детали).
          
          АРГУМЕНТЫ:
          - В 'titleOrId' передавай ТОЧНОЕ полное название книги на английском языке, которое ты получил из инструмента searchBooks.
          """)
    public String getBookDetails(String titleOrId) {
        List<BookDocument> results = catalogQueryService.search(titleOrId);

        if (results.isEmpty()) return "Книга не найдена.";

        BookDocument book = results.get(0);
        return String.format("""
                Название: %s
                Авторы: %s
                Жанры: %s
                Цена: %s BYN
                Описание: %s
                """,
                book.getTitle(),
                String.join(", ", book.getAuthors()),
                String.join(", ", book.getGenres()),
                book.getPrice(),
                book.getDescription()
        );
    }

    private String truncate(String text, int length) {
        if (text == null) return "";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }
}