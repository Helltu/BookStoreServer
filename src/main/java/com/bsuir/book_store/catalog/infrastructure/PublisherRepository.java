package com.bsuir.book_store.catalog.infrastructure;

import com.bsuir.book_store.catalog.domain.model.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PublisherRepository extends JpaRepository<Publisher, UUID> {
}