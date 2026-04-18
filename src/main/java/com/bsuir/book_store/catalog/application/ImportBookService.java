package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.domain.model.Image;
import com.bsuir.book_store.catalog.domain.model.ImageType;
import com.bsuir.book_store.catalog.infrastructure.external.google.GoogleBooksClient;
import com.bsuir.book_store.catalog.infrastructure.external.google.GoogleBooksClient.GoogleBookDto;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportBookService {

    private final GoogleBooksClient googleBooksClient;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final PublisherRepository publisherRepository;
    private final SearchSyncService searchSyncService;
    private final StorageService storageService;

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

        Publisher publisher = null;
        if (googleBook.getPublisher() != null && !googleBook.getPublisher().isBlank()) {
            publisher = publisherRepository.findByName(googleBook.getPublisher())
                    .orElseGet(() -> publisherRepository.save(
                            Publisher.builder().name(googleBook.getPublisher()).build()
                    ));
        }

        Image coverImage = null;
        if (googleBook.getImageLinks() != null && googleBook.getImageLinks().getThumbnail() != null) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                byte[] imageBytes = restTemplate.getForObject(googleBook.getImageLinks().getThumbnail(), byte[].class);
                if (imageBytes != null) {
                    try (InputStream is = new ByteArrayInputStream(imageBytes)) {
                        String coverUrl = storageService.store(is, UUID.randomUUID() + ".jpg", "image/jpeg", imageBytes.length);
                        coverImage = Image.builder().url(coverUrl).imageType(ImageType.COVER).build();
                    }
                }
            } catch (Exception e) {
                log.warn("Не удалось загрузить обложку из Google Books для ISBN {}", isbn, e);
            }
        }

        Book book = Book.builder()
                .title(googleBook.getTitle())
                .description(googleBook.getDescription())
                .isbn(isbn)
                .cost(defaultPrice)
                .stockQuantity(defaultStock)
                .authors(authors)
                .genres(genres)
                .publisher(publisher)
                .keywords(new HashSet<>())
                .coverImage(coverImage)
                .build();

        book = bookRepository.save(book);
        searchSyncService.syncBook(book);

        return book;
    }
}