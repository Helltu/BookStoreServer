package com.bsuir.book_store.catalog.api;

import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.api.dto.ImportBookRequest;
import com.bsuir.book_store.catalog.application.CatalogCommandService;
import com.bsuir.book_store.catalog.application.CatalogQueryService;
import com.bsuir.book_store.catalog.application.ImportBookService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.security.annotations.IsManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ImportBookService importBookService;
    private final CatalogCommandService commandService;
    private final CatalogQueryService queryService;

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

    @GetMapping("/search")
    public ResponseEntity<List<BookDocument>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(queryService.search(q));
    }

    @PostMapping("/books")
    @IsManager
    public ResponseEntity<UUID> createBook(@RequestBody CreateBookRequest request) {
        return ResponseEntity.ok(commandService.createBook(request));
    }
}