package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.PublisherDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.shared.exception.DomainException;
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

    @Transactional(readOnly = true)
    public List<Publisher> getAll() {
        return publisherRepository.findAll();
    }

    @Transactional
    public Publisher create(PublisherDto dto) {
        Publisher publisher = Publisher.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return publisherRepository.save(publisher);
    }

    @Transactional
    public Publisher update(UUID id, PublisherDto dto) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new DomainException("Издательство не найдено"));
        publisher.updateDetails(dto.getName(), dto.getDescription());
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