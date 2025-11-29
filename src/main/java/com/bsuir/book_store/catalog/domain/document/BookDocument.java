package com.bsuir.book_store.catalog.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Document(indexName = "books")
public class BookDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String isbn;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Text)
    private List<String> authors;

    @Field(type = FieldType.Keyword)
    private List<String> genres;
}