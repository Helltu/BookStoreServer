package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    boolean existsByIsbn(String isbn);

    Optional<Book> findByIdAndDeletedAtIsNull(UUID id);


    @Query("SELECT MIN(b.cost) FROM Book b WHERE b.deletedAt IS NULL")
    BigDecimal findMinPrice();

    @Query("SELECT MAX(b.cost) FROM Book b WHERE b.deletedAt IS NULL")
    BigDecimal findMaxPrice();

    List<Book> findByGenres_Id(UUID genreId);
    List<Book> findByAuthors_Id(UUID authorId);
    List<Book> findByPublisher_Id(UUID publisherId);

    List<Book> findByStockQuantityLessThanEqualAndDeletedAtIsNullOrderByStockQuantityAsc(int threshold);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.stockQuantity = 0")
    long countOutOfStock();
}
