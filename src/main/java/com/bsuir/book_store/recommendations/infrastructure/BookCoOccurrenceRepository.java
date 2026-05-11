package com.bsuir.book_store.recommendations.infrastructure;

import com.bsuir.book_store.recommendations.domain.BookCoOccurrenceDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookCoOccurrenceRepository extends ElasticsearchRepository<BookCoOccurrenceDocument, String> {
}
