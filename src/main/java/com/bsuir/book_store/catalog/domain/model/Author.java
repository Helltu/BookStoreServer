package com.bsuir.book_store.catalog.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public void updateDetails(String name, String biography, String photoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (biography != null) {
            this.biography = biography.trim();
        }
        if (photoUrl != null && !photoUrl.isBlank()) {
            this.photoUrl = photoUrl.trim();
        }
    }
}
