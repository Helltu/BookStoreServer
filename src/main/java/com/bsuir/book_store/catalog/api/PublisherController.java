package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.PublisherDto;
import com.bsuir.book_store.catalog.application.PublisherService;
import com.bsuir.book_store.catalog.domain.model.Publisher;
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
@RequestMapping("/api/catalog/publishers")
@RequiredArgsConstructor
@Tag(name = "Publishers", description = "Справочник издательств")
public class PublisherController {

    private final PublisherService publisherService;

    @Operation(summary = "Получить все издательства")
    @GetMapping
    public ResponseEntity<List<Publisher>> getAll(org.springframework.security.core.Authentication authentication) {
        boolean manager = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGER"));
        return ResponseEntity.ok(manager ? publisherService.getAll() : publisherService.getAllActive());
    }

    @Operation(summary = "Получить издательство по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Publisher> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(publisherService.getById(id));
    }

    @Operation(summary = "Создать издательство")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @IsManager
    public ResponseEntity<Publisher> create(@ModelAttribute PublisherDto request) {
        return ResponseEntity.ok(publisherService.create(request));
    }

    @Operation(summary = "Обновить издательство")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @IsManager
    public ResponseEntity<Publisher> update(@PathVariable UUID id, @ModelAttribute PublisherDto request) {
        return ResponseEntity.ok(publisherService.update(id, request));
    }

    @Operation(summary = "Удалить издательство")
    @DeleteMapping("/{id}")
    @IsManager
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        publisherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Полностью удалить издательство")
    @DeleteMapping("/{id}/force")
    @IsManager
    public ResponseEntity<Void> forceDelete(@PathVariable UUID id) {
        publisherService.forceDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Восстановить издательство")
    @PostMapping("/{id}/restore")
    @IsManager
    public ResponseEntity<Publisher> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(publisherService.restore(id));
    }
}