package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogCommandService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final SearchSyncService searchSyncService;

    @Transactional
    public UUID createBook(CreateBookRequest request) {
        Set<Author> authors = new HashSet<>(authorRepository.findAllById(request.getAuthorIds()));
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));

        Book book = Book.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .isbn(request.getIsbn())
                .cost(request.getPrice())
                .stockQuantity(request.getStock())
                .authors(authors)
                .genres(genres)
                .build();

        book = bookRepository.save(book);

        searchSyncService.syncBook(book);

        return book.getId();
    }
}