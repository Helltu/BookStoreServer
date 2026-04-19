package com.bsuir.book_store.reviews.infrastructure;

import com.bsuir.book_store.reviews.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findAllByBookIdOrderByCreatedAtDesc(UUID bookId);
    boolean existsByBookIdAndUserUsername(UUID bookId, String username);

    long countByBookId(UUID bookId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") UUID bookId);
}