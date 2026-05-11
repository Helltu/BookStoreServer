package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.AuthorDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final SearchSyncService searchSyncService;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<Author> getAll() {
        return authorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Author> getAllActive() {
        return authorRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public Author getById(UUID id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new DomainException("Автор не найден"));
    }

    @Transactional
    public Author create(AuthorDto dto) {
        String photoUrl = null;
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            photoUrl = storageService.store(dto.getPhoto());
        }

        Author author = Author.builder()
                .name(dto.getName())
                .biography(dto.getBiography())
                .photoUrl(photoUrl)
                .build();
        return authorRepository.save(author);
    }

    @Transactional
    public Author update(UUID id, AuthorDto dto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new DomainException("Автор не найден"));

        String photoUrl = null;
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            photoUrl = storageService.store(dto.getPhoto());
        }

        author.updateDetails(dto.getName(), dto.getBiography(), photoUrl);
        author = authorRepository.save(author);

        List<Book> affectedBooks = bookRepository.findByAuthors_Id(id);
        affectedBooks.forEach(searchSyncService::syncBook);

        return author;
    }

    @Transactional
    public void delete(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new DomainException("Автор не найден"));
        if (bookRepository.findByAuthors_Id(id).isEmpty()) {
            authorRepository.delete(author);
        } else {
            author.softDelete();
            authorRepository.save(author);
        }
    }

    @Transactional
    public void forceDelete(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new DomainException("Автор не найден"));
        List<Book> books = bookRepository.findByAuthors_Id(id);
        books.forEach(b -> {
            b.removeAuthor(author);
            bookRepository.save(b);
            searchSyncService.syncBook(b);
        });
        authorRepository.delete(author);
    }

    @Transactional
    public Author restore(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new DomainException("Автор не найден"));
        if (!author.isDeleted()) {
            throw new DomainException("Автор не удалён");
        }
        author.setDeletedAt(null);
        return authorRepository.save(author);
    }
}