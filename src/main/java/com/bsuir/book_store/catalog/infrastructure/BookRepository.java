package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    boolean existsByIsbn(String isbn);

    List<Book> findByGenres_Id(UUID genreId);
    List<Book> findByAuthors_Id(UUID authorId);
    List<Book> findByPublisher_Id(UUID publisherId);
}
