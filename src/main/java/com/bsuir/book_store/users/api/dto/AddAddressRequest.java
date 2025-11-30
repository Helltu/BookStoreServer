package com.bsuir.book_store.users.api.dto;

import lombok.Data;

@Data
public class AddAddressRequest {
    private String addressName;
    private String addressText;
    private Boolean isDefault;
}