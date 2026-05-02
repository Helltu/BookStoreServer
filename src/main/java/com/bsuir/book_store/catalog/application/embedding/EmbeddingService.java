package com.bsuir.book_store.catalog.application.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmbeddingService {

    public static final int DIMENSIONS = 384;

    private EmbeddingModel model;

    @PostConstruct
    public void init() {
        log.info("Loading local embedding model AllMiniLmL6V2...");
        this.model = new AllMiniLmL6V2EmbeddingModel();
        log.info("Embedding model loaded. Dimensions: {}", DIMENSIONS);
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) return null;
        Embedding embedding = model.embed(TextSegment.from(text)).content();
        return embedding.vector();
    }
}
