package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.GenreDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;
    private final SearchSyncService searchSyncService;

    @Transactional(readOnly = true)
    public List<Genre> getAll() {
        return genreRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Genre> getAllActive() {
        return genreRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public Genre getById(UUID id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new DomainException("Жанр не найден"));
    }

    @Transactional
    public Genre create(GenreDto dto) {
        if (genreRepository.findByName(dto.getName()).isPresent()) {
            throw new DomainException("Жанр с таким названием уже существует");
        }
        Genre genre = Genre.builder()
                .name(dto.getName())
                .build();
        return genreRepository.save(genre);
    }

    @Transactional
    public Genre update(UUID id, GenreDto dto) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new DomainException("Жанр не найден"));
        genreRepository.findByName(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new DomainException("Жанр с таким названием уже существует"); });
        genre.updateDetails(dto.getName());
        genre = genreRepository.save(genre);

        List<Book> affectedBooks = bookRepository.findByGenres_Id(id);
        affectedBooks.forEach(searchSyncService::syncBook);

        return genre;
    }

    @Transactional
    public void delete(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new DomainException("Жанр не найден"));
        if (bookRepository.findByGenres_Id(id).isEmpty()) {
            genreRepository.delete(genre);
        } else {
            genre.softDelete();
            genreRepository.save(genre);
        }
    }

    @Transactional
    public void forceDelete(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new DomainException("Жанр не найден"));
        List<Book> books = bookRepository.findByGenres_Id(id);
        books.forEach(b -> {
            b.removeGenre(genre);
            bookRepository.save(b);
            searchSyncService.syncBook(b);
        });
        genreRepository.delete(genre);
    }

    @Transactional
    public Genre restore(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new DomainException("Жанр не найден"));
        if (!genre.isDeleted()) {
            throw new DomainException("Жанр не удалён");
        }
        genre.setDeletedAt(null);
        return genreRepository.save(genre);
    }
}