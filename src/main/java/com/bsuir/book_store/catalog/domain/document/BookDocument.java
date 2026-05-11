package com.bsuir.book_store.catalog.domain.document;

import com.bsuir.book_store.catalog.domain.model.AgeRating;
import com.bsuir.book_store.catalog.domain.model.BookFormat;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer totalReviews;

    @Field(type = FieldType.Integer)
    private Integer totalOrders;

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

    @Field(type = FieldType.Integer, index = false)
    private Integer pagesCount;

    @Field(type = FieldType.Keyword)
    private BookFormat format;

    @Field(type = FieldType.Double, index = false)
    private Double weight;

    @Field(type = FieldType.Keyword, index = false)
    private String dimensions;

    @Field(type = FieldType.Keyword)
    private AgeRating ageRating;

    @Field(type = FieldType.Integer)
    private Integer publicationYear;

    @Field(type = FieldType.Keyword)
    private String language;

    @Field(type = FieldType.Keyword, index = false)
    private String originalLanguage;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant deletedAt;

    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> previewUrls;

    @Field(type = FieldType.Dense_Vector, dims = 384)
    private float[] descriptionEmbedding;
}
