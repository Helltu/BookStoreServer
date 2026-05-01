package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    boolean existsByIsbn(String isbn);

    @Query("SELECT MIN(b.cost) FROM Book b")
    BigDecimal findMinPrice();

    @Query("SELECT MAX(b.cost) FROM Book b")
    BigDecimal findMaxPrice();

    List<Book> findByGenres_Id(UUID genreId);
    List<Book> findByAuthors_Id(UUID authorId);
    List<Book> findByPublisher_Id(UUID publisherId);

    List<Book> findByStockQuantityLessThanEqualOrderByStockQuantityAsc(int threshold);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.stockQuantity = 0")
    long countOutOfStock();
}
