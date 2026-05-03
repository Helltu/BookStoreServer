package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.api.dto.UpdateBookRequest;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.domain.model.Image;
import com.bsuir.book_store.catalog.domain.model.ImageType;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Service
@RequiredArgsConstructor
public class CatalogCommandService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final PublisherRepository publisherRepository;
    private final SearchSyncService searchSyncService;
    private final BookTaggingService bookTaggingService;
    private final StorageService storageService;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    public UUID createBook(CreateBookRequest request, MultipartFile coverFile, List<MultipartFile> previewFiles) {
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

        Image cover = null;
        if (coverFile != null && !coverFile.isEmpty()) {
            cover = Image.builder().url(storageService.store(coverFile)).imageType(ImageType.COVER).build();
        }

        List<Image> previews = new ArrayList<>();
        if (previewFiles != null) {
            for (MultipartFile pf : previewFiles) {
                if (pf != null && !pf.isEmpty()) {
                    previews.add(Image.builder().url(storageService.store(pf)).imageType(ImageType.PREVIEW_PAGE).build());
                }
            }
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
                .coverImage(cover)
                .previewImages(previews)
                .pagesCount(request.getPagesCount())
                .format(request.getFormat())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .ageRating(request.getAgeRating())
                .publicationYear(request.getPublicationYear())
                .language(request.getLanguage())
                .originalLanguage(request.getOriginalLanguage())
                .build();

        book = bookRepository.save(book);

        searchSyncService.syncBook(book);

        return book.getId();
    }

    @Transactional
    public void updateBook(UUID id, UpdateBookRequest request, MultipartFile coverFile, List<MultipartFile> previewFiles) {
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

        Image cover = book.getCoverImage();
        if (coverFile != null && !coverFile.isEmpty()) {
            cover = Image.builder().url(storageService.store(coverFile)).imageType(ImageType.COVER).build();
        }

        List<Image> previews = book.getPreviewImages();
        List<String> keepUrls = request.getKeepPreviewUrls();
        if (keepUrls != null || previewFiles != null) {
            previews = new ArrayList<>();
            if (keepUrls != null) {
                book.getPreviewImages().stream()
                        .filter(img -> keepUrls.contains(img.getUrl()))
                        .forEach(previews::add);
            }
            if (previewFiles != null) {
                for (MultipartFile pf : previewFiles) {
                    if (pf != null && !pf.isEmpty()) {
                        previews.add(Image.builder().url(storageService.store(pf)).imageType(ImageType.PREVIEW_PAGE).build());
                    }
                }
            }
        }

        book.updateDetails(
                request.getTitle(), request.getDescription(),
                request.getPrice(), request.getStock(),
                authors, genres, publisher, keywords, cover, previews,
                request.getPagesCount(), request.getFormat(), request.getWeight(),
                request.getDimensions(), request.getAgeRating(), request.getPublicationYear(),
                request.getLanguage(), request.getOriginalLanguage()
        );

        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }

    public List<String> generateKeywordSuggestions(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        return bookTaggingService.generateTags(book.getTitle(), book.getDescription(), book.getKeywords());
    }

    public List<String> generateKeywordSuggestions(String title, String description, Set<String> existingKeywords) {
        return bookTaggingService.generateTags(title, description, existingKeywords);
    }

    public String generateDescriptionSuggestion(String title, String authors, String genres) {
        String result = bookTaggingService.generateDescription(title, authors, genres);
        if (result == null || result.isBlank()) {
            throw new DomainException("Не удалось сгенерировать описание. ИИ вернул пустой ответ.");
        }
        return result;
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

    public String generateDescriptionSuggestion(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        String authors = book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(", "));
        String genres = book.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "));

        String newDescription = bookTaggingService.generateDescription(book.getTitle(), authors, genres);

        if (newDescription == null || newDescription.isBlank()) {
            throw new DomainException("Не удалось сгенерировать описание. ИИ вернул пустой ответ.");
        }
        return newDescription;
    }

    @Transactional
    public void adjustStock(UUID bookId, int quantity) {
        if (quantity < 0) throw new DomainException("Количество на складе не может быть отрицательным");
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));
        book.updateDetails(null, null, null, quantity, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        bookRepository.save(book);
        searchSyncService.syncBook(book);
    }

    @Transactional
    public void deleteBook(UUID id) {
        bookRepository.deleteById(id);
        elasticsearchOperations.delete(id.toString(), BookDocument.class);
    }
}