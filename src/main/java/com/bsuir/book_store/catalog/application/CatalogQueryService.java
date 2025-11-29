package com.bsuir.book_store.catalog.application;

import com.bsuir.book_store.catalog.domain.document.BookDocument;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final BookElasticRepository elasticRepository;

    public List<BookDocument> search(String query) {
        if (query == null || query.isBlank()) {
            List<BookDocument> result = new ArrayList<>();
            elasticRepository.findAll().forEach(result::add);
            return result;
        }

        return elasticRepository.searchByComplexQuery(query);
    }
}