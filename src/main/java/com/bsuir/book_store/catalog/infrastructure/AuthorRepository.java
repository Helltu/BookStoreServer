package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {
    Optional<Author> findByName(String name);
}