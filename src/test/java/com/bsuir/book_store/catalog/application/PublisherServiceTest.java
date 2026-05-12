package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.api.dto.PublisherDto;
import com.bsuir.book_store.catalog.application.sync.SearchSyncService;
import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.domain.model.Publisher;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.catalog.infrastructure.PublisherRepository;
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
class PublisherServiceTest {

    @Mock private PublisherRepository publisherRepository;
    @Mock private BookRepository bookRepository;
    @Mock private SearchSyncService searchSyncService;
    @Mock private StorageService storageService;

    @InjectMocks
    private PublisherService publisherService;

    private UUID publisherId;
    private Publisher publisher;

    @BeforeEach
    void setUp() {
        publisherId = UUID.randomUUID();
        publisher = Publisher.builder().id(publisherId).name("Penguin").description("Big publisher").build();
    }

    @Test
    void createShouldThrowOnDuplicateName() {
        PublisherDto dto = new PublisherDto();
        dto.setName("Penguin");
        when(publisherRepository.findByName("Penguin")).thenReturn(Optional.of(publisher));

        assertThrows(DomainException.class, () -> publisherService.create(dto));
        verify(publisherRepository, never()).save(any());
    }

    @Test
    void createShouldSavePublisherWithoutLogo() {
        PublisherDto dto = new PublisherDto();
        dto.setName("Penguin");
        when(publisherRepository.findByName("Penguin")).thenReturn(Optional.empty());
        when(publisherRepository.save(any(Publisher.class))).thenReturn(publisher);

        Publisher result = publisherService.create(dto);

        assertEquals("Penguin", result.getName());
        verify(storageService, never()).store(any());
    }

    @Test
    void createShouldUploadLogoWhenProvided() {
        PublisherDto dto = new PublisherDto();
        dto.setName("Penguin");
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        dto.setLogo(mockFile);
        when(publisherRepository.findByName("Penguin")).thenReturn(Optional.empty());
        when(storageService.store(mockFile)).thenReturn("http://storage/logo.png");
        when(publisherRepository.save(any())).thenReturn(publisher);

        publisherService.create(dto);

        verify(storageService).store(mockFile);
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> publisherService.getById(publisherId));
    }

    @Test
    void updateShouldThrowOnDuplicateNameFromOtherPublisher() {
        UUID otherId = UUID.randomUUID();
        Publisher other = Publisher.builder().id(otherId).name("Oxford").build();
        PublisherDto dto = new PublisherDto();
        dto.setName("Oxford");

        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(publisherRepository.findByName("Oxford")).thenReturn(Optional.of(other));

        assertThrows(DomainException.class, () -> publisherService.update(publisherId, dto));
    }

    @Test
    void updateShouldSaveAndSyncBooks() {
        PublisherDto dto = new PublisherDto();
        dto.setName("Penguin Updated");
        Book book = Book.builder().title("Some Book").cost(BigDecimal.TEN).stockQuantity(3).build();

        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(publisherRepository.findByName("Penguin Updated")).thenReturn(Optional.empty());
        when(publisherRepository.save(publisher)).thenReturn(publisher);
        when(bookRepository.findByPublisher_Id(publisherId)).thenReturn(List.of(book));

        publisherService.update(publisherId, dto);

        verify(publisherRepository).save(publisher);
        verify(searchSyncService).syncBook(book);
    }

    @Test
    void deleteShouldHardDeleteWhenNoBooksLinked() {
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(bookRepository.findByPublisher_Id(publisherId)).thenReturn(List.of());

        publisherService.delete(publisherId);

        verify(publisherRepository).delete(publisher);
    }

    @Test
    void deleteShouldSoftDeleteWhenBooksExist() {
        Book book = Book.builder().title("Some Book").cost(BigDecimal.TEN).stockQuantity(3).build();
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(bookRepository.findByPublisher_Id(publisherId)).thenReturn(List.of(book));

        publisherService.delete(publisherId);

        assertTrue(publisher.isDeleted());
        verify(publisherRepository).save(publisher);
        verify(publisherRepository, never()).delete(any());
    }

    @Test
    void forceDeleteShouldDetachFromBooksAndDelete() {
        Book book = Book.builder().title("Some Book").cost(BigDecimal.TEN).stockQuantity(3).build();
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(bookRepository.findByPublisher_Id(publisherId)).thenReturn(List.of(book));
        when(bookRepository.save(any())).thenReturn(book);

        publisherService.forceDelete(publisherId);

        verify(bookRepository).save(book);
        verify(searchSyncService).syncBook(book);
        verify(publisherRepository).delete(publisher);
    }

    @Test
    void restoreShouldClearDeletedAt() {
        publisher.softDelete();
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(publisherRepository.save(publisher)).thenReturn(publisher);

        publisherService.restore(publisherId);

        verify(publisherRepository).save(publisher);
    }

    @Test
    void restoreShouldThrowWhenNotDeleted() {
        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));

        assertThrows(DomainException.class, () -> publisherService.restore(publisherId));
    }
}
