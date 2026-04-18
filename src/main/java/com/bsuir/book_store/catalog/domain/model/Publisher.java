package com.bsuir.book_store.catalog.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "publishers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    public void updateDetails(String name, String description, String logoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (logoUrl != null && !logoUrl.isBlank()) {
            this.logoUrl = logoUrl.trim();
        }
    }
}