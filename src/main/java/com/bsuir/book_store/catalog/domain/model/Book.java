package com.bsuir.book_store.catalog.domain.model;

import com.bsuir.book_store.shared.exception.DomainException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "book_genres",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "book_keywords", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "keyword")
    private Set<String> keywords = new HashSet<>();

    @Column(name = "pages_count")
    private Integer pagesCount;

    @Column(name = "format")
    @Enumerated(EnumType.STRING)
    private BookFormat format;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "dimensions")
    private String dimensions;

    @Column(name = "age_rating")
    @Enumerated(EnumType.STRING)
    private AgeRating ageRating;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "language")
    private String language;

    @Column(name = "original_language")
    private String originalLanguage;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cover_image_id")
    private Image coverImage;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
            name = "book_previews",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "image_id")
    )
    private List<Image> previewImages = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public void reserveStock(int quantity) {
        if (quantity <= 0) throw new DomainException("Количество должно быть положительным");
        if (this.stockQuantity < quantity) {
            throw new DomainException("Недостаточно товара '" + title + "' на складе. Доступно: " + stockQuantity);
        }
        this.stockQuantity -= quantity;
    }

    public void releaseStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) throw new DomainException("Цена не может быть отрицательной");
        this.cost = newPrice;
    }

    public void updateDetails(String title, String description, BigDecimal cost, Integer stockQuantity, Set<Author> authors, Set<Genre> genres, Publisher publisher, Set<String> keywords, Image coverImage, List<Image> previewImages, Integer pagesCount, BookFormat format, Double weight, String dimensions, AgeRating ageRating, Integer publicationYear, String language, String originalLanguage) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (cost != null) {
            this.updatePrice(cost);
        }
        if (stockQuantity != null) {
            this.stockQuantity = stockQuantity;
        }
        if (authors != null) {
            this.authors = authors;
        }
        if (genres != null) {
            this.genres = genres;
        }
        if (publisher != null) {
            this.publisher = publisher;
        }

        if (keywords != null) {
            if (this.keywords == null) {
                this.keywords = new HashSet<>();
            }
            this.keywords.clear();
            this.keywords.addAll(keywords);
        }
        if (coverImage != null) {
            this.coverImage = coverImage;
        }
        if (previewImages != null) {
            if (this.previewImages == null) {
                this.previewImages = new ArrayList<>();
            }
            this.previewImages.clear();
            this.previewImages.addAll(previewImages);
        }
        
        if (pagesCount != null) {
            this.pagesCount = pagesCount;
        }
        if (format != null) {
            this.format = format;
        }
        if (weight != null) {
            this.weight = weight;
        }
        if (dimensions != null) {
            this.dimensions = dimensions;
        }
        if (ageRating != null) {
            this.ageRating = ageRating;
        }
        if (publicationYear != null) {
            this.publicationYear = publicationYear;
        }
        if (language != null) {
            this.language = language;
        }
        if (originalLanguage != null) {
            this.originalLanguage = originalLanguage;
        }
    }

    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            if (this.keywords == null) this.keywords = new HashSet<>();
            this.keywords.add(keyword.trim());
        }
    }

    public void removeKeyword(String keyword) {
        if (keyword != null && this.keywords != null) {
            this.keywords.remove(keyword.trim());
        }
    }

    public void updateDescription(String description) {
        if (description != null && !description.isBlank()) {
            this.description = description.trim();
        }
    }

    public void updateRating(double averageRating, int totalReviews) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }
}
