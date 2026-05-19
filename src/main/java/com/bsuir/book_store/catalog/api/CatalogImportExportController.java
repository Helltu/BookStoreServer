package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.export.AuthorExportDto;
import com.bsuir.book_store.catalog.api.dto.export.BookExportDto;
import com.bsuir.book_store.catalog.api.dto.export.GenreExportDto;
import com.bsuir.book_store.catalog.api.dto.export.PublisherExportDto;
import com.bsuir.book_store.catalog.application.CatalogImportExportService;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog/io")
@RequiredArgsConstructor
@Tag(name = "Импорт/Экспорт каталога", description = "Импорт и экспорт сущностей каталога в формате JSON")
@SecurityRequirement(name = "JWT")
public class CatalogImportExportController {

    private final CatalogImportExportService importExportService;

    @GetMapping("/export/authors")
    @IsManager
    @Operation(summary = "Экспорт всех авторов в формате JSON")
    public ResponseEntity<List<AuthorExportDto>> exportAuthors() {
        return ResponseEntity.ok(importExportService.exportAuthors());
    }

    @GetMapping("/export/genres")
    @IsManager
    @Operation(summary = "Экспорт всех жанров в формате JSON")
    public ResponseEntity<List<GenreExportDto>> exportGenres() {
        return ResponseEntity.ok(importExportService.exportGenres());
    }

    @GetMapping("/export/publishers")
    @IsManager
    @Operation(summary = "Экспорт всех издательств в формате JSON")
    public ResponseEntity<List<PublisherExportDto>> exportPublishers() {
        return ResponseEntity.ok(importExportService.exportPublishers());
    }

    @GetMapping("/export/books")
    @IsManager
    @Operation(summary = "Экспорт всех книг в формате JSON (авторы, жанры и издательства указаны по ID)")
    public ResponseEntity<List<BookExportDto>> exportBooks() {
        return ResponseEntity.ok(importExportService.exportBooks());
    }

    @PostMapping(value = "/import/authors", consumes = "application/json")
    @IsManager
    @Operation(summary = "Импорт авторов из JSON. Существующие ID пропускаются.")
    public ResponseEntity<Map<String, Integer>> importAuthors(@RequestBody List<AuthorExportDto> dtos) {
        int imported = importExportService.importAuthors(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping(value = "/import/genres", consumes = "application/json")
    @IsManager
    @Operation(summary = "Импорт жанров из JSON. Существующие ID пропускаются.")
    public ResponseEntity<Map<String, Integer>> importGenres(@RequestBody List<GenreExportDto> dtos) {
        int imported = importExportService.importGenres(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping(value = "/import/publishers", consumes = "application/json")
    @IsManager
    @Operation(summary = "Импорт издательств из JSON. Существующие ID пропускаются.")
    public ResponseEntity<Map<String, Integer>> importPublishers(@RequestBody List<PublisherExportDto> dtos) {
        int imported = importExportService.importPublishers(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping(value = "/import/books", consumes = "application/json")
    @IsManager
    @Operation(summary = "Импорт книг из JSON. Связанные авторы, жанры и издательства должны уже существовать в БД.")
    public ResponseEntity<Map<String, Integer>> importBooks(@RequestBody List<BookExportDto> dtos) {
        int imported = importExportService.importBooks(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping("/reindex/books")
    @IsManager
    @Operation(summary = "Переиндексация всех книг в Elasticsearch из БД (маппинг индекса сохраняется).")
    public ResponseEntity<Map<String, Integer>> reindexBooks() {
        int reindexed = importExportService.reindexAllBooks();
        return ResponseEntity.ok(Map.of("reindexed", reindexed));
    }

    @PostMapping("/reindex/books/recreate")
    @IsManager
    @Operation(summary = "Удалить индекс книг, создать заново с текущим маппингом (включая эмбеддинги) и переиндексировать все книги из БД.")
    public ResponseEntity<Map<String, Integer>> recreateAndReindexBooks() {
        int reindexed = importExportService.recreateAndReindexBooks();
        return ResponseEntity.ok(Map.of("reindexed", reindexed));
    }

    @DeleteMapping("/index/books")
    @IsManager
    @Operation(summary = "Удалить индекс книг и пересоздать его пустым с текущим маппингом. Данные НЕ переиндексируются.")
    public ResponseEntity<Map<String, String>> recreateEmptyBookIndex() {
        importExportService.recreateBookIndex();
        return ResponseEntity.ok(Map.of("status", "index recreated, empty"));
    }
}
