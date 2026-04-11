package com.bsuir.book_store.users.api.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String newPassword;
}