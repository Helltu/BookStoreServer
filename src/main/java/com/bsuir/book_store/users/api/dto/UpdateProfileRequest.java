package com.bsuir.book_store.users.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на обновление профиля (оставьте пустым для сохранения старых значений)")
public class UpdateProfileRequest {
    @Schema(description = "Имя", example = "")
    private String firstName;
    @Schema(description = "Фамилия", example = "")
    private String lastName;
    @Schema(description = "Телефон", example = "")
    private String phone;
}