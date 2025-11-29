package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.domain.model.Image;
import com.bsuir.book_store.catalog.domain.model.ImageType;
import com.bsuir.book_store.catalog.infrastructure.external.google.GoogleBooksClient;
import com.bsuir.book_store.catalog.infrastructure.external.google.GoogleBooksClient.GoogleBookDto;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.catalog.infrastructure.ImageRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportBookService {

    private final GoogleBooksClient googleBooksClient;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ImageRepository imageRepository;
    private final SearchSyncService searchSyncService;

    @Transactional
    public Book importBook(String isbn, BigDecimal defaultPrice, int defaultStock) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DomainException("Книга с ISBN " + isbn + " уже существует в каталоге");
        }

        GoogleBookDto googleBook = googleBooksClient.fetchBookByIsbn(isbn);

        Set<Author> authors = new HashSet<>();
        if (googleBook.getAuthors() != null) {
            for (String authorName : googleBook.getAuthors()) {
                Author author = authorRepository.findByName(authorName)
                        .orElseGet(() -> authorRepository.save(
                                Author.builder().name(authorName).build()
                        ));
                authors.add(author);
            }
        }

        Set<Genre> genres = new HashSet<>();
        if (googleBook.getCategories() != null) {
            for (String categoryName : googleBook.getCategories()) {
                Genre genre = genreRepository.findByName(categoryName)
                        .orElseGet(() -> genreRepository.save(
                                Genre.builder().name(categoryName).build()
                        ));
                genres.add(genre);
            }
        }

        if (googleBook.getImageLinks() != null && googleBook.getImageLinks().getThumbnail() != null) {
            Image cover = Image.builder()
                    .url(googleBook.getImageLinks().getThumbnail())
                    .imageType(ImageType.COVER)
                    .build();
            imageRepository.save(cover);
        }

        Book book = Book.builder()
                .title(googleBook.getTitle())
                .description(googleBook.getDescription())
                .isbn(isbn)
                .cost(defaultPrice)
                .stockQuantity(defaultStock)
                .authors(authors)
                .genres(genres)
                .build();

        book = bookRepository.save(book);
        searchSyncService.syncBook(book);

        return book;
    }
}