package com.bsuir.book_store.assistant.api;

import com.bsuir.book_store.assistant.api.dto.ChatRequest;
import com.bsuir.book_store.assistant.api.dto.ChatResponse;
import com.bsuir.book_store.assistant.application.BookStoreAssistant;
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
public class ChatController {

    private final BookStoreAssistant assistant;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String aiResponse = assistant.chat(userDetails.getUsername(), request.getMessage());

        return ResponseEntity.ok(new ChatResponse(aiResponse));
    }
}