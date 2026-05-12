package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.GenreDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Genre;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.shared.exception.DomainException;
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
class GenreServiceTest {

    @Mock private GenreRepository genreRepository;
    @Mock private BookRepository bookRepository;
    @Mock private SearchSyncService searchSyncService;

    @InjectMocks
    private GenreService genreService;

    private UUID genreId;
    private Genre genre;

    @BeforeEach
    void setUp() {
        genreId = UUID.randomUUID();
        genre = Genre.builder().id(genreId).name("Fantasy").build();
    }

    @Test
    void createShouldSaveGenre() {
        GenreDto dto = new GenreDto();
        dto.setName("Fantasy");
        when(genreRepository.findByName("Fantasy")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenReturn(genre);

        Genre result = genreService.create(dto);

        assertEquals("Fantasy", result.getName());
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void createShouldThrowOnDuplicateName() {
        GenreDto dto = new GenreDto();
        dto.setName("Fantasy");
        when(genreRepository.findByName("Fantasy")).thenReturn(Optional.of(genre));

        assertThrows(DomainException.class, () -> genreService.create(dto));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void getByIdShouldReturnGenre() {
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));

        Genre result = genreService.getById(genreId);

        assertEquals("Fantasy", result.getName());
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(genreRepository.findById(genreId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> genreService.getById(genreId));
    }

    @Test
    void updateShouldUpdateNameAndSyncBooks() {
        GenreDto dto = new GenreDto();
        dto.setName("Sci-Fi");
        Book book = Book.builder().title("Dune").cost(BigDecimal.TEN).stockQuantity(1).build();

        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(genreRepository.findByName("Sci-Fi")).thenReturn(Optional.empty());
        when(genreRepository.save(genre)).thenReturn(genre);
        when(bookRepository.findByGenres_Id(genreId)).thenReturn(List.of(book));

        genreService.update(genreId, dto);

        verify(genreRepository).save(genre);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void updateShouldThrowOnDuplicateNameFromOtherGenre() {
        UUID otherId = UUID.randomUUID();
        Genre other = Genre.builder().id(otherId).name("Sci-Fi").build();
        GenreDto dto = new GenreDto();
        dto.setName("Sci-Fi");

        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(genreRepository.findByName("Sci-Fi")).thenReturn(Optional.of(other));

        assertThrows(DomainException.class, () -> genreService.update(genreId, dto));
    }

    @Test
    void deleteShouldHardDeleteWhenNoBooksLinked() {
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(bookRepository.findByGenres_Id(genreId)).thenReturn(List.of());

        genreService.delete(genreId);

        verify(genreRepository).delete(genre);
    }

    @Test
    void deleteShouldSoftDeleteWhenBooksExist() {
        Book book = Book.builder().title("Book").cost(BigDecimal.TEN).stockQuantity(1).build();
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(bookRepository.findByGenres_Id(genreId)).thenReturn(List.of(book));

        genreService.delete(genreId);

        assertTrue(genre.isDeleted());
        verify(genreRepository).save(genre);
        verify(genreRepository, never()).delete(any());
    }

    @Test
    void forceDeleteShouldDetachFromBooksAndDelete() {
        Book book = Book.builder().title("Book").cost(BigDecimal.TEN).stockQuantity(1)
                .genres(new java.util.HashSet<>()).build();
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(bookRepository.findByGenres_Id(genreId)).thenReturn(List.of(book));
        when(bookRepository.save(any())).thenReturn(book);

        genreService.forceDelete(genreId);

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
        verify(genreRepository).delete(genre);
    }

    @Test
    void restoreShouldClearDeletedAt() {
        genre.softDelete();
        assertTrue(genre.isDeleted());
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(genreRepository.save(genre)).thenReturn(genre);

        genreService.restore(genreId);

        verify(genreRepository).save(genre);
    }

    @Test
    void restoreShouldThrowWhenNotDeleted() {
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));

        assertThrows(DomainException.class, () -> genreService.restore(genreId));
    }
}
