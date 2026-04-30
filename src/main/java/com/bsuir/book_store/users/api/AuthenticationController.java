package com.bsuir.book_store.users.api;

import com.bsuir.book_store.users.api.dto.AuthenticationRequest;
import com.bsuir.book_store.users.api.dto.AuthenticationResponse;
import com.bsuir.book_store.users.api.dto.RegisterRequest;
import com.bsuir.book_store.users.api.dto.VerifyEmailRequest;
import com.bsuir.book_store.users.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация и вход пользователей")
@SecurityRequirements()
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Регистрация", description = "Отправляет код подтверждения на email. Для завершения вызовите /verify-email.")
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        authenticationService.register(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Подтверждение email", description = "Принимает email и 6-значный код (действителен 1 минуту). Возвращает JWT.")
    @PostMapping("/verify-email")
    public ResponseEntity<AuthenticationResponse> verifyEmail(
            @RequestBody @Valid VerifyEmailRequest request
    ) {
        return ResponseEntity.ok(authenticationService.completeRegistration(request.getEmail(), request.getCode()));
    }

    @Operation(summary = "Таймер кода", description = "Возвращает количество секунд до истечения кода. 0 — код истёк или не существует.")
    @GetMapping("/verification-timer")
    public ResponseEntity<Map<String, Long>> getVerificationTimer(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("secondsRemaining", authenticationService.getVerificationSecondsRemaining(email)));
    }

    @Operation(summary = "Повторная отправка кода", description = "Доступно только после истечения предыдущего кода.")
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestParam String email) {
        authenticationService.resendVerification(email);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Вход в систему", description = "Проверяет логин/пароль и выдает JWT токен.")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}