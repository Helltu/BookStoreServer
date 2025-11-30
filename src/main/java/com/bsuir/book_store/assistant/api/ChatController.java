package com.bsuir.book_store.assistant.api;

import com.bsuir.book_store.assistant.api.dto.ChatRequest;
import com.bsuir.book_store.assistant.api.dto.ChatResponse;
import com.bsuir.book_store.assistant.application.BookStoreAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Чат с умным консультантом")
public class ChatController {

    private final BookStoreAssistant assistant;

    @Operation(summary = "Отправить сообщение", description = "Обработка запроса пользователя через LLM (OpenAI)")
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String aiResponse = assistant.chat(userDetails.getUsername(), request.getMessage());

        return ResponseEntity.ok(new ChatResponse(aiResponse));
    }
}