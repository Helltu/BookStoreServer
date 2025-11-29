package com.bsuir.book_store.catalog.infrastructure.elastic;


import org.springframework.data.elasticsearch.annotations.Query;
import com.bsuir.book_store.catalog.domain.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface BookElasticRepository extends ElasticsearchRepository<BookDocument, String> {
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"authors^2\", \"description\"], \"fuzziness\": \"AUTO\"}}")
    List<BookDocument> searchByComplexQuery(String query);
    List<BookDocument> findByGenresIn(List<String> genres);
}