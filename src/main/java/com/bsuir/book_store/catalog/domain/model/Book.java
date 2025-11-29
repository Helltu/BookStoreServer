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
import java.util.HashSet;
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
}