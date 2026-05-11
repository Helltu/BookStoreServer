package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.PublisherDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublisherService {
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;
    private final SearchSyncService searchSyncService;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<Publisher> getAll() {
        return publisherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Publisher> getAllActive() {
        return publisherRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public Publisher getById(UUID id) {
        return publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
    }

    @Transactional
    public Publisher create(PublisherDto dto) {
        if (publisherRepository.findByName(dto.getName()).isPresent()) {
            throw new DomainException("Издательство с таким названием уже существует");
        }
        String logoUrl = null;
        if (dto.getLogo() != null && !dto.getLogo().isEmpty()) {
            logoUrl = storageService.store(dto.getLogo());
        }

        Publisher publisher = Publisher.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(logoUrl)
                .build();
        return publisherRepository.save(publisher);
    }

    @Transactional
    public Publisher update(UUID id, PublisherDto dto) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
        publisherRepository.findByName(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new DomainException("Издательство с таким названием уже существует"); });

        String logoUrl = null;
        if (dto.getLogo() != null && !dto.getLogo().isEmpty()) {
            logoUrl = storageService.store(dto.getLogo());
        }

        publisher.updateDetails(dto.getName(), dto.getDescription(), logoUrl);
        publisher = publisherRepository.save(publisher);

        List<Book> affectedBooks = bookRepository.findByPublisher_Id(id);
        affectedBooks.forEach(searchSyncService::syncBook);

        return publisher;
    }

    @Transactional
    public void delete(UUID id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
        if (bookRepository.findByPublisher_Id(id).isEmpty()) {
            publisherRepository.delete(publisher);
        } else {
            publisher.softDelete();
            publisherRepository.save(publisher);
        }
    }

    @Transactional
    public void forceDelete(UUID id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
        List<Book> books = bookRepository.findByPublisher_Id(id);
        books.forEach(b -> {
            b.detachPublisher();
            bookRepository.save(b);
            searchSyncService.syncBook(b);
        });
        publisherRepository.delete(publisher);
    }

    @Transactional
    public Publisher restore(UUID id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
        if (!publisher.isDeleted()) {
            throw new DomainException("Издательство не удалено");
        }
        publisher.setDeletedAt(null);
        return publisherRepository.save(publisher);
    }
}