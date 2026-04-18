package com.bsuir.book_store.assistant.api.dto;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос к AI ассистенту")
public class ChatRequest {
    @Schema(description = "Сообщение", example = "")
    private String message;
}