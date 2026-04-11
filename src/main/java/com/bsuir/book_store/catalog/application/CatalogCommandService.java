package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.api.dto.UpdateBookRequest;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.shared.exception.DomainException;
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
    private final PublisherRepository publisherRepository;
    private final SearchSyncService searchSyncService;

    @Transactional
    public UUID createBook(CreateBookRequest request) {
        Set<Author> authors = new HashSet<>(authorRepository.findAllById(request.getAuthorIds()));
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));

        Publisher publisher = null;
        if (request.getPublisherId() != null) {
            publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new DomainException("Издательство не найдено"));
        }

        Book book = Book.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .isbn(request.getIsbn())
                .cost(request.getPrice())
                .stockQuantity(request.getStock())
                .authors(authors)
                .genres(genres)
                .publisher(publisher)
                .build();

        book = bookRepository.save(book);

        searchSyncService.syncBook(book);

        return book.getId();
    }

    @Transactional
    public void updateBook(UUID id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        Set<Author> authors = new HashSet<>(authorRepository.findAllById(request.getAuthorIds()));
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));

        Publisher publisher = null;
        if (request.getPublisherId() != null) {
            publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new DomainException("Издательство не найдено"));
        }

        book.updateDetails(
                request.getTitle(), request.getDescription(),
                request.getPrice(), request.getStock(),
                authors, genres, publisher
        );

        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }
}