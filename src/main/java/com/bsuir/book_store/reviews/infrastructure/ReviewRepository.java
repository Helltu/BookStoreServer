package com.bsuir.book_store.reviews.infrastructure;

import com.bsuir.book_store.reviews.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findAllByBookIdOrderByCreatedAtDesc(UUID bookId);
    boolean existsByBookIdAndUserUsername(UUID bookId, String username);
}