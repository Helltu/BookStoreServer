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
@Schema(description = "Запрос на авторизацию")
public class AuthenticationRequest {
    @Schema(description = "Имя пользователя", example = "")
    private String username;

    @Schema(description = "Пароль", example = "")
    private String password;
}
