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
import java.util.List;
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
    private final BookTaggingService bookTaggingService;

    @Transactional
    public UUID createBook(CreateBookRequest request) {
        List<UUID> reqAuthorIds = request.getAuthorIds() != null ? request.getAuthorIds() : List.of();
        List<UUID> reqGenreIds = request.getGenreIds() != null ? request.getGenreIds() : List.of();

        Set<Author> authors = new HashSet<>(authorRepository.findAllById(reqAuthorIds));
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(reqGenreIds));

        Publisher publisher = null;
        if (request.getPublisherId() != null) {
            publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new DomainException("Издательство не найдено"));
        }

        Set<String> keywords = new HashSet<>();
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            keywords.addAll(request.getKeywords());
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
                .keywords(keywords)
                .build();

        book = bookRepository.save(book);

        searchSyncService.syncBook(book);

        return book.getId();
    }

    @Transactional
    public void updateBook(UUID id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        Set<Author> authors = request.getAuthorIds() != null
                ? new HashSet<>(authorRepository.findAllById(request.getAuthorIds()))
                : book.getAuthors();

        Set<Genre> genres = request.getGenreIds() != null
                ? new HashSet<>(genreRepository.findAllById(request.getGenreIds()))
                : book.getGenres();

        Publisher publisher = book.getPublisher();
        if (request.getPublisherId() != null) {
            publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new DomainException("Издательство не найдено"));
        }

        Set<String> keywords = request.getKeywords() != null
                ? new HashSet<>(request.getKeywords())
                : book.getKeywords();

        book.updateDetails(
                request.getTitle(), request.getDescription(),
                request.getPrice(), request.getStock(),
                authors, genres, publisher, keywords
        );

        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }

    @Transactional
    public void generateAndAddKeywords(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        List<String> newTags = bookTaggingService.generateTags(book.getTitle(), book.getDescription(), book.getKeywords());
        newTags.forEach(book::addKeyword);

        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }

    @Transactional
    public void addKeyword(UUID bookId, String keyword) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));
        book.addKeyword(keyword);
        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }

    @Transactional
    public void removeKeyword(UUID bookId, String keyword) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));
        book.removeKeyword(keyword);
        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }
}