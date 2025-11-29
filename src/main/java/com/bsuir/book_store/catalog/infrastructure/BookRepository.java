package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    boolean existsByIsbn(String isbn);
}
