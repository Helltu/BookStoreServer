package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.export.AuthorExportDto;
import com.bsuir.book_store.catalog.api.dto.export.BookExportDto;
import com.bsuir.book_store.catalog.api.dto.export.GenreExportDto;
import com.bsuir.book_store.catalog.api.dto.export.PublisherExportDto;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogImportExportService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final PublisherRepository publisherRepository;
    private final EntityManager entityManager;
    private final SearchSyncService searchSyncService;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional(readOnly = true)
    public List<AuthorExportDto> exportAuthors() {
        return authorRepository.findAll().stream()
                .map(a -> new AuthorExportDto(a.getId(), a.getName(), a.getBiography(), a.getPhotoUrl()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GenreExportDto> exportGenres() {
        return genreRepository.findAll().stream()
                .map(g -> new GenreExportDto(g.getId(), g.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PublisherExportDto> exportPublishers() {
        return publisherRepository.findAll().stream()
                .map(p -> new PublisherExportDto(p.getId(), p.getName(), p.getDescription(), p.getLogoUrl()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookExportDto> exportBooks() {
        return bookRepository.findAll().stream()
                .map(b -> new BookExportDto(
                        b.getId(),
                        b.getTitle(),
                        b.getIsbn(),
                        b.getDescription(),
                        b.getStockQuantity(),
                        b.getCost(),
                        b.getPublisher() != null ? b.getPublisher().getId() : null,
                        b.getAuthors().stream().map(Author::getId).collect(Collectors.toList()),
                        b.getGenres().stream().map(Genre::getId).collect(Collectors.toList()),
                        b.getKeywords(),
                        b.getPagesCount(),
                        b.getFormat(),
                        b.getWeight(),
                        b.getDimensions(),
                        b.getAgeRating(),
                        b.getPublicationYear(),
                        b.getLanguage(),
                        b.getOriginalLanguage()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public int importAuthors(List<AuthorExportDto> dtos) {
        int count = 0;
        Set<UUID> existingIds = authorRepository.findAllById(
                dtos.stream().map(AuthorExportDto::id).collect(Collectors.toList())).stream()
                .map(Author::getId).collect(Collectors.toCollection(HashSet::new));
        Set<UUID> seen = new HashSet<>();
        for (AuthorExportDto dto : dtos) {
            if (existingIds.contains(dto.id())) continue;
            if (!seen.add(dto.id())) continue;
            entityManager.createNativeQuery(
                    "INSERT INTO authors (id, name, biography, photo_url) VALUES (?1, ?2, ?3, ?4)")
                    .setParameter(1, dto.id())
                    .setParameter(2, dto.name() != null ? dto.name().trim() : null)
                    .setParameter(3, dto.biography() != null ? dto.biography().trim() : null)
                    .setParameter(4, dto.photoUrl() != null ? dto.photoUrl().trim() : null)
                    .executeUpdate();
            count++;
        }
        return count;
    }

    @Transactional
    public int importGenres(List<GenreExportDto> dtos) {
        int count = 0;
        List<UUID> inputIds = dtos.stream().map(GenreExportDto::id).collect(Collectors.toList());
        Set<UUID> existingIds = genreRepository.findAllById(inputIds).stream()
                .map(Genre::getId).collect(Collectors.toCollection(HashSet::new));
        Set<UUID> seen = new HashSet<>();
        for (GenreExportDto dto : dtos) {
            if (existingIds.contains(dto.id())) continue;
            if (!seen.add(dto.id())) continue;
            entityManager.createNativeQuery(
                    "INSERT INTO genres (id, name) VALUES (?1, ?2)")
                    .setParameter(1, dto.id())
                    .setParameter(2, dto.name() != null ? dto.name().trim() : null)
                    .executeUpdate();
            count++;
        }
        return count;
    }

    @Transactional
    public int importPublishers(List<PublisherExportDto> dtos) {
        int count = 0;
        Set<UUID> existingIds = publisherRepository.findAllById(
                dtos.stream().map(PublisherExportDto::id).collect(Collectors.toList())).stream()
                .map(Publisher::getId).collect(Collectors.toCollection(HashSet::new));
        Set<UUID> seen = new HashSet<>();
        for (PublisherExportDto dto : dtos) {
            if (existingIds.contains(dto.id())) continue;
            if (!seen.add(dto.id())) continue;
            entityManager.createNativeQuery(
                    "INSERT INTO publishers (id, name, description, logo_url) VALUES (?1, ?2, ?3, ?4)")
                    .setParameter(1, dto.id())
                    .setParameter(2, dto.name() != null ? dto.name().trim() : null)
                    .setParameter(3, dto.description() != null ? dto.description().trim() : null)
                    .setParameter(4, dto.logoUrl() != null ? dto.logoUrl().trim() : null)
                    .executeUpdate();
            count++;
        }
        return count;
    }

    @Transactional
    public int importBooks(List<BookExportDto> dtos) {
        int count = 0;
        Set<UUID> existingIds = bookRepository.findAllById(
                dtos.stream().map(BookExportDto::id).collect(Collectors.toList())).stream()
                .map(Book::getId).collect(Collectors.toCollection(HashSet::new));
        Set<UUID> seen = new HashSet<>();
        Set<UUID> insertedIds = new HashSet<>();
        for (BookExportDto dto : dtos) {
            if (existingIds.contains(dto.id())) continue;
            if (!seen.add(dto.id())) continue;

            if (dto.publisherId() != null && !publisherRepository.existsById(dto.publisherId())) {
                throw new DomainException(
                        "Publisher not found: " + dto.publisherId() + ". Import publishers first.");
            }
            for (UUID authorId : dto.authorIds()) {
                if (!authorRepository.existsById(authorId)) {
                    throw new DomainException(
                            "Author not found: " + authorId + ". Import authors first.");
                }
            }
            for (UUID genreId : dto.genreIds()) {
                if (!genreRepository.existsById(genreId)) {
                    throw new DomainException(
                            "Genre not found: " + genreId + ". Import genres first.");
                }
            }

            entityManager.createNativeQuery(
                    "INSERT INTO books (id, title, isbn, description, stock_quantity, cost, publisher_id, average_rating, total_reviews, pages_count, format, weight, dimensions, age_rating, publication_year, language, original_language, created_at, updated_at) " +
                            "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 0, 0, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, NOW(), NOW())")
                    .setParameter(1, dto.id())
                    .setParameter(2, dto.title())
                    .setParameter(3, dto.isbn())
                    .setParameter(4, dto.description())
                    .setParameter(5, dto.stockQuantity())
                    .setParameter(6, dto.cost())
                    .setParameter(7, dto.publisherId())
                    .setParameter(8, dto.pagesCount())
                    .setParameter(9, dto.format() != null ? dto.format().name() : null)
                    .setParameter(10, dto.weight())
                    .setParameter(11, dto.dimensions())
                    .setParameter(12, dto.ageRating() != null ? dto.ageRating().name() : null)
                    .setParameter(13, dto.publicationYear())
                    .setParameter(14, dto.language())
                    .setParameter(15, dto.originalLanguage())
                    .executeUpdate();

            for (UUID authorId : dto.authorIds()) {
                entityManager.createNativeQuery(
                        "INSERT INTO book_authors (book_id, author_id) VALUES (?1, ?2)")
                        .setParameter(1, dto.id())
                        .setParameter(2, authorId)
                        .executeUpdate();
            }
            for (UUID genreId : dto.genreIds()) {
                entityManager.createNativeQuery(
                        "INSERT INTO book_genres (book_id, genre_id) VALUES (?1, ?2)")
                        .setParameter(1, dto.id())
                        .setParameter(2, genreId)
                        .executeUpdate();
            }
            if (dto.keywords() != null) {
                for (String kw : dto.keywords()) {
                    entityManager.createNativeQuery(
                            "INSERT INTO book_keywords (book_id, keyword) VALUES (?1, ?2)")
                            .setParameter(1, dto.id())
                            .setParameter(2, kw)
                            .executeUpdate();
                }
            }
            insertedIds.add(dto.id());
            count++;
        }
        entityManager.flush();
        entityManager.clear();
        for (UUID id : insertedIds) {
            bookRepository.findById(id).ifPresent(searchSyncService::syncBook);
        }
        return count;
    }

    @Transactional
    public int reindexAllBooks() {
        List<Book> all = bookRepository.findAll();
        for (Book book : all) {
            searchSyncService.syncBook(book);
        }
        return all.size();
    }

    public void recreateBookIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(BookDocument.class);
        if (indexOps.exists()) {
            log.info("Deleting existing 'books' index...");
            indexOps.delete();
        }
        log.info("Creating fresh 'books' index with current mapping...");
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping(BookDocument.class));
    }

    @Transactional
    public int recreateAndReindexBooks() {
        recreateBookIndex();
        return reindexAllBooks();
    }
}
