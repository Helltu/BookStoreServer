package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.AuthorDto;
import com.bsuir.book_store.catalog.application.AuthorService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Справочник авторов")
public class AuthorController {

    private final AuthorService authorService;

    @Operation(summary = "Получить всех авторов")
    @GetMapping
    public ResponseEntity<List<Author>> getAll() {
        return ResponseEntity.ok(authorService.getAll());
    }

    @Operation(summary = "Получить автора по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Author> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(authorService.getById(id));
    }

    @Operation(summary = "Создать автора")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @IsManager
    public ResponseEntity<Author> create(@ModelAttribute AuthorDto request) {
        return ResponseEntity.ok(authorService.create(request));
    }

    @Operation(summary = "Обновить автора")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @IsManager
    public ResponseEntity<Author> update(@PathVariable UUID id, @ModelAttribute AuthorDto request) {
        return ResponseEntity.ok(authorService.update(id, request));
    }

    @Operation(summary = "Удалить автора")
    @DeleteMapping("/{id}")
    @IsManager
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}