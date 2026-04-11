package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.GenreDto;
import com.bsuir.book_store.catalog.domain.model.Genre;
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

    @Transactional(readOnly = true)
    public List<Genre> getAll() {
        return genreRepository.findAll();
    }

    @Transactional
    public Genre create(GenreDto dto) {
        Genre genre = Genre.builder()
                .name(dto.getName())
                .build();
        return genreRepository.save(genre);
    }

    @Transactional
    public Genre update(UUID id, GenreDto dto) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new DomainException("Жанр не найден"));
        genre.setName(dto.getName());
        return genreRepository.save(genre);
    }

    @Transactional
    public void delete(UUID id) {
        if (!genreRepository.existsById(id)) {
            throw new DomainException("Жанр не найден");
        }
        genreRepository.deleteById(id);
    }
}