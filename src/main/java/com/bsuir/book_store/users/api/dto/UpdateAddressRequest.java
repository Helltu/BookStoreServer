package com.bsuir.book_store.users.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на обновление адреса")
public class UpdateAddressRequest {
    @Schema(description = "Название адреса (например: Дом)", example = "")
    private String addressName;
    @Schema(description = "Полный адрес", example = "")
    private String addressText;
    @Schema(description = "Сделать адресом по умолчанию?", example = "false")
    private Boolean isDefault;
}
