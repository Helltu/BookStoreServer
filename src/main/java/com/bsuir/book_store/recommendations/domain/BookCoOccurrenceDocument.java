package com.bsuir.book_store.recommendations.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "book_co_occurrence")
public class BookCoOccurrenceDocument {

    @Id
    private String bookAId;

    @Field(type = FieldType.Object)
    private List<Pair> topPairs;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pair {
        @Field(type = FieldType.Keyword)
        private String bookBId;

        @Field(type = FieldType.Long)
        private Long count;
    }
}
