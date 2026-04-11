package com.bsuir.book_store.catalog.infrastructure.elastic;


import com.bsuir.book_store.catalog.domain.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookElasticRepository extends ElasticsearchRepository<BookDocument, String> {
}