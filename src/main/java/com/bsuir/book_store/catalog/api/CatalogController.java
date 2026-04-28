package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.BookSearchCriteria;
import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.api.dto.ImportBookRequest;
import com.bsuir.book_store.catalog.api.dto.PriceRangeResponse;
import com.bsuir.book_store.catalog.api.dto.StockAdjustRequest;
import com.bsuir.book_store.catalog.api.dto.UpdateBookRequest;
import com.bsuir.book_store.catalog.api.dto.GenerateDescriptionRequest;
import com.bsuir.book_store.catalog.api.dto.GenerateKeywordsRequest;
import com.bsuir.book_store.catalog.api.dto.KeywordRequest;
import com.bsuir.book_store.catalog.application.CatalogCommandService;
import com.bsuir.book_store.catalog.application.CatalogQueryService;
import com.bsuir.book_store.catalog.application.ImportBookService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Поиск книг и управление ассортиментом")
public class CatalogController {

    private final ImportBookService importBookService;
    private final CatalogCommandService commandService;
    private final CatalogQueryService queryService;

    @Operation(summary = "Импорт книги (Менеджер)", description = "Загрузка книги из Google Books API по ISBN")
    @PostMapping("/import")
    @IsManager
    public ResponseEntity<String> importBookFromExternalApi(@RequestBody ImportBookRequest request) {
        Book importedBook = importBookService.importBook(
                request.getIsbn(),
                request.getPrice(),
                request.getStock()
        );
        return ResponseEntity.ok("Книга '" + importedBook.getTitle() + "' успешно импортирована с ID: " + importedBook.getId());
    }

    @Operation(summary = "Диапазон цен", description = "Минимальная и максимальная цена в каталоге для слайдера фильтрации")
    @GetMapping("/price-range")
    public ResponseEntity<PriceRangeResponse> getPriceRange() {
        return ResponseEntity.ok(queryService.getPriceRange());
    }

    @Operation(summary = "Поиск книг", description = "Полнотекстовый поиск c фильтрами и пагинацией (Query Params)")
    @GetMapping("/search")
    public ResponseEntity<Page<BookDocument>> search(@ModelAttribute BookSearchCriteria criteria,
                                                     Pageable pageable) {
        return ResponseEntity.ok(queryService.search(criteria, pageable));
    }

    @Operation(summary = "Получить книгу по ID", description = "Возвращает карточку конкретной книги")
    @GetMapping("/books/{id}")
    public ResponseEntity<BookDocument> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getBookById(id.toString()));
    }

    @Operation(summary = "Создание книги", description = "Создает новую книгу по введенным данным")
    @PostMapping(value = "/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @IsManager
    public ResponseEntity<UUID> createBook(
            @ModelAttribute CreateBookRequest request
    ) {
        return ResponseEntity.ok(commandService.createBook(request, request.getCoverFile(), request.getPreviewFiles()));
    }

    @Operation(summary = "Обновление книги", description = "Изменение данных книги (кроме ISBN)")
    @PutMapping(value = "/books/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @IsManager
    public ResponseEntity<Void> updateBook(
            @PathVariable UUID id, 
            @ModelAttribute UpdateBookRequest request
    ) {
        commandService.updateBook(id, request, request.getCoverFile(), request.getPreviewFiles());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Установить количество на складе (Менеджер)", description = "Устанавливает абсолютное значение stock для книги")
    @PatchMapping("/books/{id}/stock")
    @IsManager
    public ResponseEntity<Void> adjustStock(@PathVariable UUID id, @RequestBody StockAdjustRequest request) {
        commandService.adjustStock(id, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удаление книги", description = "Удаляет книгу из каталога и Elasticsearch")
    @DeleteMapping("/books/{id}")
    @IsManager
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        commandService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Сгенерировать теги (Менеджер)", description = "ИИ генерирует новые ключевые слова для книги с учетом уже существующих — возвращает предложения без сохранения")
    @GetMapping("/books/{id}/keywords/generate")
    @IsManager
    public ResponseEntity<List<String>> generateKeywords(@PathVariable UUID id) {
        return ResponseEntity.ok(commandService.generateKeywordSuggestions(id));
    }

    @Operation(summary = "Сгенерировать теги без id (Менеджер)", description = "ИИ генерирует ключевые слова по переданным данным — для новой книги до сохранения")
    @PostMapping("/books/keywords/suggest")
    @IsManager
    public ResponseEntity<List<String>> suggestKeywords(@RequestBody GenerateKeywordsRequest request) {
        return ResponseEntity.ok(commandService.generateKeywordSuggestions(
                request.getTitle(), request.getDescription(), request.getExistingKeywords()));
    }

    @Operation(summary = "Добавить ключевое слово (Менеджер)", description = "Ручное добавление одного ключевого слова")
    @PostMapping("/books/{id}/keywords")
    @IsManager
    public ResponseEntity<Void> addKeyword(@PathVariable UUID id, @RequestBody KeywordRequest request) {
        commandService.addKeyword(id, request.getKeyword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить ключевое слово (Менеджер)", description = "Удаление ключевого слова у книги")
    @DeleteMapping("/books/{id}/keywords/{keyword}")
    @IsManager
    public ResponseEntity<Void> removeKeyword(@PathVariable UUID id, @PathVariable String keyword) {
        commandService.removeKeyword(id, keyword);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Сгенерировать описание (Менеджер)", description = "ИИ генерирует аннотацию для книги — возвращает предложение без сохранения")
    @GetMapping("/books/{id}/description/generate")
    @IsManager
    public ResponseEntity<String> generateDescription(@PathVariable UUID id) {
        return ResponseEntity.ok(commandService.generateDescriptionSuggestion(id));
    }

    @Operation(summary = "Сгенерировать описание без id (Менеджер)", description = "ИИ генерирует аннотацию по переданным данным — для новой книги до сохранения")
    @PostMapping("/books/description/suggest")
    @IsManager
    public ResponseEntity<String> suggestDescription(@RequestBody GenerateDescriptionRequest request) {
        return ResponseEntity.ok(commandService.generateDescriptionSuggestion(
                request.getTitle(), request.getAuthors(), request.getGenres()));
    }
}