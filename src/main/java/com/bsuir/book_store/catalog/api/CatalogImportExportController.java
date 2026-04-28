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
@Tag(name = "Catalog Import/Export", description = "JSON import and export for catalog entities")
@SecurityRequirement(name = "JWT")
public class CatalogImportExportController {

    private final CatalogImportExportService importExportService;

    // ---- EXPORT ----

    @GetMapping("/export/authors")
    @IsManager
    @Operation(summary = "Export all authors as JSON")
    public ResponseEntity<List<AuthorExportDto>> exportAuthors() {
        return ResponseEntity.ok(importExportService.exportAuthors());
    }

    @GetMapping("/export/genres")
    @IsManager
    @Operation(summary = "Export all genres as JSON")
    public ResponseEntity<List<GenreExportDto>> exportGenres() {
        return ResponseEntity.ok(importExportService.exportGenres());
    }

    @GetMapping("/export/publishers")
    @IsManager
    @Operation(summary = "Export all publishers as JSON")
    public ResponseEntity<List<PublisherExportDto>> exportPublishers() {
        return ResponseEntity.ok(importExportService.exportPublishers());
    }

    @GetMapping("/export/books")
    @IsManager
    @Operation(summary = "Export all books as JSON (references authors/genres/publishers by ID)")
    public ResponseEntity<List<BookExportDto>> exportBooks() {
        return ResponseEntity.ok(importExportService.exportBooks());
    }

    // ---- IMPORT ----

    @PostMapping(value = "/import/authors", consumes = "application/json")
    @IsManager
    @Operation(summary = "Import authors from JSON. Skips existing IDs.")
    public ResponseEntity<Map<String, Integer>> importAuthors(@RequestBody List<AuthorExportDto> dtos) {
        int imported = importExportService.importAuthors(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping(value = "/import/genres", consumes = "application/json")
    @IsManager
    @Operation(summary = "Import genres from JSON. Skips existing IDs.")
    public ResponseEntity<Map<String, Integer>> importGenres(@RequestBody List<GenreExportDto> dtos) {
        int imported = importExportService.importGenres(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping(value = "/import/publishers", consumes = "application/json")
    @IsManager
    @Operation(summary = "Import publishers from JSON. Skips existing IDs.")
    public ResponseEntity<Map<String, Integer>> importPublishers(@RequestBody List<PublisherExportDto> dtos) {
        int imported = importExportService.importPublishers(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping(value = "/import/books", consumes = "application/json")
    @IsManager
    @Operation(summary = "Import books from JSON. Requires referenced publishers, authors, and genres to already exist.")
    public ResponseEntity<Map<String, Integer>> importBooks(@RequestBody List<BookExportDto> dtos) {
        int imported = importExportService.importBooks(dtos);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @PostMapping("/reindex/books")
    @IsManager
    @Operation(summary = "Reindex all books in Elasticsearch from the database.")
    public ResponseEntity<Map<String, Integer>> reindexBooks() {
        int reindexed = importExportService.reindexAllBooks();
        return ResponseEntity.ok(Map.of("reindexed", reindexed));
    }
}
