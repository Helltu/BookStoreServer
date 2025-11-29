package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    Optional<Genre> findByName(String name);
}