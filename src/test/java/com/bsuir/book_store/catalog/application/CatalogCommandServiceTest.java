package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.CreateBookRequest;
import com.bsuir.book_store.catalog.api.dto.UpdateBookRequest;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.infrastructure.AuthorRepository;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.GenreRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.storage.StorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogCommandServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private SearchSyncService searchSyncService;
    @Mock private BookTaggingService bookTaggingService;
    @Mock private StorageService storageService;
    @Mock private ElasticsearchOperations elasticsearchOperations;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private CatalogCommandService catalogCommandService;

    private UUID bookId;
    private Book book;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        book = Book.builder()
                .id(bookId)
                .title("Clean Code")
                .cost(new BigDecimal("29.99"))
                .stockQuantity(10)
                .build();
    }

    private CreateBookRequest buildCreateRequest(String title, BigDecimal price, int stock) {
        CreateBookRequest r = new CreateBookRequest();
        r.setTitle(title);
        r.setPrice(price);
        r.setStock(stock);
        return r;
    }

    @Test
    void createBookShouldSaveAndSync() {
        when(authorRepository.findAllById(any())).thenReturn(List.of());
        when(genreRepository.findAllById(any())).thenReturn(List.of());
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any());

        catalogCommandService.createBook(buildCreateRequest("Clean Code", new BigDecimal("29.99"), 10), null, null);

        verify(bookRepository).save(any(Book.class));
        verify(searchSyncService).syncBook(any(Book.class));
    }

    @Test
    void createBookShouldThrowWhenPublisherNotFound() {
        UUID publisherId = UUID.randomUUID();
        CreateBookRequest r = buildCreateRequest("Book", BigDecimal.TEN, 5);
        r.setPublisherId(publisherId);

        when(authorRepository.findAllById(any())).thenReturn(List.of());
        when(genreRepository.findAllById(any())).thenReturn(List.of());
        when(publisherRepository.findByIdAndDeletedAtIsNull(publisherId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> catalogCommandService.createBook(r, null, null));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBookShouldThrowWhenNotFound() {
        when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> catalogCommandService.updateBook(bookId, new UpdateBookRequest(), null, null));
    }

    @Test
    void updateBookShouldSaveAndSync() {
        UpdateBookRequest r = new UpdateBookRequest();
        r.setTitle("Refactoring");

        when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);

        catalogCommandService.updateBook(bookId, r, null, null);

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void adjustStockShouldUpdateAndSync() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);

        catalogCommandService.adjustStock(bookId, 20);

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void adjustStockShouldThrowOnNegativeQuantity() {
        assertThrows(DomainException.class, () -> catalogCommandService.adjustStock(bookId, -1));
        verify(bookRepository, never()).findById(any());
    }

    @Test
    void deleteBookShouldSoftDeleteWhenOrdersExist() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(orderRepository.existsAnyOrderForBook(bookId)).thenReturn(true);
        when(bookRepository.save(book)).thenReturn(book);

        catalogCommandService.deleteBook(bookId);

        assertTrue(book.isDeleted());
        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void deleteBookShouldHardDeleteWhenNoOrders() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(orderRepository.existsAnyOrderForBook(bookId)).thenReturn(false);

        catalogCommandService.deleteBook(bookId);

        verify(bookRepository).deleteById(bookId);
        verify(elasticsearchOperations).delete(bookId.toString(), com.bsuir.book_store.catalog.domain.document.BookDocument.class);
    }

    @Test
    void forceDeleteBookShouldThrowWhenOrdersExist() {
        when(bookRepository.existsById(bookId)).thenReturn(true);
        when(orderRepository.existsAnyOrderForBook(bookId)).thenReturn(true);

        assertThrows(DomainException.class, () -> catalogCommandService.forceDeleteBook(bookId));
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void forceDeleteBookShouldThrowWhenNotFound() {
        when(bookRepository.existsById(bookId)).thenReturn(false);

        assertThrows(DomainException.class, () -> catalogCommandService.forceDeleteBook(bookId));
    }

    @Test
    void restoreBookShouldClearDeletedAt() {
        book.softDelete();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);

        catalogCommandService.restoreBook(bookId);

        assertFalse(book.isDeleted());
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void restoreBookShouldThrowWhenNotDeleted() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThrows(DomainException.class, () -> catalogCommandService.restoreBook(bookId));
    }

    @Test
    void addKeywordShouldSaveAndSync() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);

        catalogCommandService.addKeyword(bookId, "clean");

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void removeKeywordShouldSaveAndSync() {
        book.addKeyword("clean");
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);

        catalogCommandService.removeKeyword(bookId, "clean");

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void createBookShouldUsePublisherWhenProvided() {
        UUID publisherId = UUID.randomUUID();
        Publisher publisher = Publisher.builder().id(publisherId).name("Penguin").build();
        CreateBookRequest r = buildCreateRequest("Book", BigDecimal.TEN, 5);
        r.setPublisherId(publisherId);

        when(authorRepository.findAllById(any())).thenReturn(List.of());
        when(genreRepository.findAllById(any())).thenReturn(List.of());
        when(publisherRepository.findByIdAndDeletedAtIsNull(publisherId)).thenReturn(Optional.of(publisher));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any());

        catalogCommandService.createBook(r, null, null);

        verify(publisherRepository).findByIdAndDeletedAtIsNull(publisherId);
        verify(bookRepository).save(any(Book.class));
    }
}
