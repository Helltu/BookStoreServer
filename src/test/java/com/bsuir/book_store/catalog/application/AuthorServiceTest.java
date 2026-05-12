package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.AuthorDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Author;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock private AuthorRepository authorRepository;
    @Mock private BookRepository bookRepository;
    @Mock private SearchSyncService searchSyncService;
    @Mock private StorageService storageService;

    @InjectMocks
    private AuthorService authorService;

    private UUID authorId;
    private Author author;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        author = Author.builder().id(authorId).name("Tolkien").biography("Bio").build();
    }

    @Test
    void createShouldSaveAuthorWithoutPhoto() {
        AuthorDto dto = new AuthorDto();
        dto.setName("Tolkien");
        dto.setBiography("Bio");
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        Author result = authorService.create(dto);

        assertEquals("Tolkien", result.getName());
        verify(storageService, never()).store(any());
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void createShouldUploadPhotoWhenProvided() {
        AuthorDto dto = new AuthorDto();
        dto.setName("Tolkien");
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        dto.setPhoto(mockFile);
        when(storageService.store(mockFile)).thenReturn("http://storage/photo.jpg");
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        authorService.create(dto);

        verify(storageService).store(mockFile);
    }

    @Test
    void getByIdShouldReturnAuthor() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        Author result = authorService.getById(authorId);

        assertEquals("Tolkien", result.getName());
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> authorService.getById(authorId));
    }

    @Test
    void updateShouldSaveAndSyncBooks() {
        AuthorDto dto = new AuthorDto();
        dto.setName("Tolkien Updated");
        Book book = Book.builder().title("LOTR").cost(BigDecimal.TEN).stockQuantity(5).build();

        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);
        when(bookRepository.findByAuthors_Id(authorId)).thenReturn(List.of(book));

        authorService.update(authorId, dto);

        verify(authorRepository).save(author);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void deleteShouldHardDeleteWhenNoBooksLinked() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthors_Id(authorId)).thenReturn(List.of());

        authorService.delete(authorId);

        verify(authorRepository).delete(author);
    }

    @Test
    void deleteShouldSoftDeleteWhenBooksExist() {
        Book book = Book.builder().title("LOTR").cost(BigDecimal.TEN).stockQuantity(5).build();
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthors_Id(authorId)).thenReturn(List.of(book));

        authorService.delete(authorId);

        assertTrue(author.isDeleted());
        verify(authorRepository).save(author);
        verify(authorRepository, never()).delete(any());
    }

    @Test
    void forceDeleteShouldDetachFromBooksAndDelete() {
        Book book = Book.builder().title("LOTR").cost(BigDecimal.TEN).stockQuantity(5)
                .authors(new java.util.HashSet<>()).build();
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthors_Id(authorId)).thenReturn(List.of(book));
        when(bookRepository.save(any())).thenReturn(book);

        authorService.forceDelete(authorId);

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
        verify(authorRepository).delete(author);
    }

    @Test
    void restoreShouldClearDeletedAt() {
        author.softDelete();
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);

        authorService.restore(authorId);

        verify(authorRepository).save(author);
    }

    @Test
    void restoreShouldThrowWhenNotDeleted() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        assertThrows(DomainException.class, () -> authorService.restore(authorId));
    }
}
