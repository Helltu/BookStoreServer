package com.bsuir.book_store.users.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на регистрацию")
public class RegisterRequest {
    @Schema(description = "Уникальный логин", example = "")
    private String username;

    @Schema(description = "Электронная почта", example = "")
    private String email;

    @Schema(description = "Пароль (минимум 8 символов, буквы и цифры)", example = "")
    private String password;

    @Schema(description = "Имя (опционально)", example = "")
    private String firstName;

    @Schema(description = "Фамилия (опционально)", example = "")
    private String lastName;

    @Schema(description = "Контактный телефон (опционально)", example = "")
    private String phone;
}