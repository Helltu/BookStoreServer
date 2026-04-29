package com.bsuir.book_store.users.api;

import com.bsuir.book_store.users.api.dto.AddAddressRequest;
import com.bsuir.book_store.users.api.dto.ChangePasswordRequest;
import com.bsuir.book_store.users.api.dto.UpdateAddressRequest;
import com.bsuir.book_store.users.api.dto.UpdateProfileRequest;
import com.bsuir.book_store.users.api.dto.UserProfileResponse;
import com.bsuir.book_store.users.api.dto.WishlistBookDto;
import com.bsuir.book_store.users.application.UserService;
import com.bsuir.book_store.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @Operation(summary = "Изменить пароль", description = "Обновляет пароль текущего пользователя")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.changePassword(userDetails.getUsername(), request.getCurrentPassword(), request.getNewPassword());
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

    @Operation(summary = "Обновить адрес", description = "Обновляет существующий адрес доставки")
    @PatchMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> updateAddress(
            @PathVariable UUID addressId,
            @RequestBody UpdateAddressRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.updateAddress(userDetails.getUsername(), addressId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить адрес", description = "Удаляет адрес из профиля пользователя")
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.deleteAddress(userDetails.getUsername(), addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Добавить книгу в избранное")
    @PostMapping("/me/wishlist/{bookId}")
    public ResponseEntity<Void> addToWishlist(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.addBookToWishlist(userDetails.getUsername(), bookId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить книгу из избранного")
    @DeleteMapping("/me/wishlist/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.removeBookFromWishlist(userDetails.getUsername(), bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить список желаемого")
    @GetMapping("/me/wishlist")
    public ResponseEntity<List<WishlistBookDto>> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getWishlist(userDetails.getUsername()));
    }
}