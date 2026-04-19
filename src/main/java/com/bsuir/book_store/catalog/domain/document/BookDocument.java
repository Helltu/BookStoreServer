package com.bsuir.book_store.catalog.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Document(indexName = "books")
public class BookDocument {

    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String isbn;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer totalReviews;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private List<String> authors;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private List<String> genres;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String publisher;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private List<String> keywords;

    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> previewUrls;
}