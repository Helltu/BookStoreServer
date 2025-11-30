package com.bsuir.book_store.users.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AddressDto {
    private UUID id;
    private String addressName;
    private String addressText;
    private Boolean isDefault;
}