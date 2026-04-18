package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.BookSearchCriteria;
import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.api.dto.ImportBookRequest;
import com.bsuir.book_store.catalog.api.dto.UpdateBookRequest;
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

    @Operation(summary = "Поиск книг", description = "Полнотекстовый поиск c фильтрами и пагинацией (Query Params)")
    @GetMapping("/search")
    public ResponseEntity<Page<BookDocument>> search(@ModelAttribute BookSearchCriteria criteria,
                                                     Pageable pageable) {
        return ResponseEntity.ok(queryService.search(criteria, pageable));
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

    @Operation(summary = "Сгенерировать теги (Менеджер)", description = "ИИ генерирует новые ключевые слова для книги с учетом уже существующих")
    @PostMapping("/books/{id}/keywords/generate")
    @IsManager
    public ResponseEntity<Void> generateKeywords(@PathVariable UUID id) {
        commandService.generateAndAddKeywords(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Добавить ключевое слово (Менеджер)", description = "Ручное добавление одного ключевого слова")
    @PostMapping("/books/{id}/keywords")
    @IsManager
    public ResponseEntity<Void> addKeyword(@PathVariable UUID id, @RequestParam String keyword) {
        commandService.addKeyword(id, keyword);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить ключевое слово (Менеджер)", description = "Удаление ключевого слова у книги")
    @DeleteMapping("/books/{id}/keywords")
    @IsManager
    public ResponseEntity<Void> removeKeyword(@PathVariable UUID id, @RequestParam String keyword) {
        commandService.removeKeyword(id, keyword);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Сгенерировать описание (Менеджер)", description = "ИИ генерирует красивую аннотацию для книги на основе названия, авторов и жанров")
    @PostMapping("/books/{id}/description/generate")
    @IsManager
    public ResponseEntity<Void> generateDescription(@PathVariable UUID id) {
        commandService.generateAndSetDescription(id);
        return ResponseEntity.ok().build();
    }
}