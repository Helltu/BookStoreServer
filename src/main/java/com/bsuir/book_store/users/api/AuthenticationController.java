package com.bsuir.book_store.users.api;

import com.bsuir.book_store.users.api.dto.AuthenticationRequest;
import com.bsuir.book_store.users.api.dto.AuthenticationResponse;
import com.bsuir.book_store.users.api.dto.RegisterRequest;
import com.bsuir.book_store.users.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация и вход пользователей")
@SecurityRequirements()
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Регистрация", description = "Регистрирует нового клиента. Пароль шифруется. Email проверяется на уникальность.")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @Operation(summary = "Вход в систему", description = "Проверяет логин/пароль и выдает JWT токен.")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}