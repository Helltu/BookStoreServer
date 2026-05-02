package com.bsuir.book_store.reviews.api.dto;

import com.bsuir.book_store.reviews.domain.Review;
import lombok.Value;

import java.sql.Timestamp;
import java.util.UUID;

@Value
public class ReviewResponse {

    UUID id;
    Integer rating;
    String text;
    Timestamp createdAt;
    AuthorDto author;

    @Value
    public static class AuthorDto {
        String username;
        String firstName;
        String lastName;
    }

    public static ReviewResponse from(Review review) {
        var user = review.getUser();
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getText(),
                review.getCreatedAt(),
                new AuthorDto(user.getUsername(), user.getFirstName(), user.getLastName())
        );
    }
}
