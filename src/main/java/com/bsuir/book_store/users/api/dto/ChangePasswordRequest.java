package com.bsuir.book_store.users.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на изменение пароля")
public class ChangePasswordRequest {
    @Schema(description = "Новый пароль", example = "")
    private String newPassword;
}