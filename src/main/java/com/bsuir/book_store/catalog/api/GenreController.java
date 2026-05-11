package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.GenreDto;
import com.bsuir.book_store.catalog.application.GenreService;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/genres")
@RequiredArgsConstructor
@Tag(name = "Genres", description = "Справочник жанров")
public class GenreController {

    private final GenreService genreService;

    @Operation(summary = "Получить все жанры")
    @GetMapping
    public ResponseEntity<List<Genre>> getAll(org.springframework.security.core.Authentication authentication) {
        boolean manager = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGER"));
        return ResponseEntity.ok(manager ? genreService.getAll() : genreService.getAllActive());
    }

    @Operation(summary = "Получить жанр по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Genre> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(genreService.getById(id));
    }

    @Operation(summary = "Создать жанр")
    @PostMapping
    @IsManager
    public ResponseEntity<Genre> create(@RequestBody GenreDto request) {
        return ResponseEntity.ok(genreService.create(request));
    }

    @Operation(summary = "Обновить жанр")
    @PutMapping("/{id}")
    @IsManager
    public ResponseEntity<Genre> update(@PathVariable UUID id, @RequestBody GenreDto request) {
        return ResponseEntity.ok(genreService.update(id, request));
    }

    @Operation(summary = "Удалить жанр")
    @DeleteMapping("/{id}")
    @IsManager
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        genreService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Полностью удалить жанр")
    @DeleteMapping("/{id}/force")
    @IsManager
    public ResponseEntity<Void> forceDelete(@PathVariable UUID id) {
        genreService.forceDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Восстановить жанр")
    @PostMapping("/{id}/restore")
    @IsManager
    public ResponseEntity<Genre> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(genreService.restore(id));
    }
}