package com.bsuir.book_store.catalog.infrastructure.external.google;

import com.bsuir.book_store.shared.exception.DomainException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleBooksClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GOOGLE_BOOKS_API_URL = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    public GoogleBookDto fetchBookByIsbn(String isbn) {
        String url = GOOGLE_BOOKS_API_URL + isbn;

        try {
            GoogleResponse response = restTemplate.getForObject(url, GoogleResponse.class);

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                throw new DomainException("Книга с ISBN " + isbn + " не найдена в Google Books");
            }

            return response.getItems().get(0).getVolumeInfo();

        } catch (Exception e) {
            throw new DomainException("Ошибка при обращении к Google Books API: " + e.getMessage());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleResponse {
        private List<Item> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private GoogleBookDto volumeInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleBookDto {
        private String title;
        private List<String> authors;
        private String description;
        private List<String> categories;
        private List<IndustryIdentifier> industryIdentifiers;
        private ImageLinks imageLinks;
        private Integer pageCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryIdentifier {
        private String type;
        private String identifier;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinks {
        private String thumbnail;
    }
}