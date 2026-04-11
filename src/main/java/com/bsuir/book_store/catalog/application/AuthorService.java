package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.AuthorDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.shared.exception.DomainException;
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

    @Transactional(readOnly = true)
    public List<Author> getAll() {
        return authorRepository.findAll();
    }

    @Transactional
    public Author create(AuthorDto dto) {
        Author author = Author.builder()
                .name(dto.getName())
                .biography(dto.getBiography())
                .build();
        return authorRepository.save(author);
    }

    @Transactional
    public Author update(UUID id, AuthorDto dto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new DomainException("Автор не найден"));
        author.setName(dto.getName());
        author.setBiography(dto.getBiography());
        author = authorRepository.save(author);

        List<Book> affectedBooks = bookRepository.findByAuthors_Id(id);
        affectedBooks.forEach(searchSyncService::syncBook);

        return author;
    }

    @Transactional
    public void delete(UUID id) {
        if (!authorRepository.existsById(id)) {
            throw new DomainException("Автор не найден");
        }
        authorRepository.deleteById(id);
    }
}