package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {
}