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
    public Publisher getById(UUID id) {
        return publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
    }

    @Transactional
    public Publisher create(PublisherDto dto) {
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
        if (!publisherRepository.existsById(id)) {
            throw new DomainException("Издательство не найдено");
        }
        publisherRepository.deleteById(id);
    }
}