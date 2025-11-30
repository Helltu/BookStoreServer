package com.bsuir.book_store.users.api.dto;

import com.bsuir.book_store.users.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String contactPhone;
    private String firstName;
    private String lastName;
    private Role role;
    private List<AddressDto> addresses;
}