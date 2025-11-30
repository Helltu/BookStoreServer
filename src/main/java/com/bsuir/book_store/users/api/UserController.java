package com.bsuir.book_store.users.api;

import com.bsuir.book_store.users.api.dto.AddAddressRequest;
import com.bsuir.book_store.users.api.dto.UpdateProfileRequest;
import com.bsuir.book_store.users.api.dto.UserProfileResponse;
import com.bsuir.book_store.users.application.UserService;
import com.bsuir.book_store.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Управление профилем пользователя")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить мой профиль")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    @Operation(summary = "Обновить профиль", description = "Изменить имя, фамилию или телефон")
    @PatchMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Добавить адрес", description = "Добавляет новый адрес доставки")
    @PostMapping("/me/addresses")
    public ResponseEntity<Void> addAddress(
            @RequestBody AddAddressRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.addAddress(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}