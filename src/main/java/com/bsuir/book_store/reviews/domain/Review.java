package com.bsuir.book_store.reviews.domain;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public static Review leave(User user, Book book, int rating, String text) {
        validateRating(rating);
        validateText(text);

        Review review = new Review();
        review.user = user;
        review.book = book;
        review.rating = rating;
        review.text = text;

        return review;
    }

    public void updateContent(int newRating, String newText) {
        validateRating(newRating);
        validateText(newText);

        this.rating = newRating;
        this.text = newText;
    }

    private static void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new DomainException("Рейтинг должен быть от 1 до 5");
        }
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException("Текст отзыва не может быть пустым");
        }
    }
}