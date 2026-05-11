package com.bsuir.book_store.users.application;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.api.dto.AddAddressRequest;
import com.bsuir.book_store.users.api.dto.AddressDto;
import com.bsuir.book_store.users.api.dto.UpdateAddressRequest;
import com.bsuir.book_store.users.api.dto.UpdateProfileRequest;
import com.bsuir.book_store.users.api.dto.UserProfileResponse;
import com.bsuir.book_store.users.api.dto.WishlistBookDto;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.UserAddress;
import com.bsuir.book_store.orders.application.OrderEmailService;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderEmailService orderEmailService;

    @Transactional
    public void updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));

        user.updateProfile(request.getFirstName(), request.getLastName(), request.getPhone());

        userRepository.save(user);
    }

    @Transactional
    public void addAddress(String username, AddAddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));

        UserAddress address = UserAddress.builder()
                .addressName(request.getAddressName())
                .addressText(request.getAddressText())
                .isDefault(request.getIsDefault())
                .build();

        user.addAddress(address);

        userRepository.save(user);
    }

    @Transactional
    public void updateAddress(String username, UUID addressId, UpdateAddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));
        user.updateAddress(addressId, request.getAddressName(), request.getAddressText(), request.getIsDefault());
        userRepository.save(user);
    }

    @Transactional
    public void deleteAddress(String username, UUID addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));
        user.removeAddress(addressId);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new DomainException("Неверный текущий пароль");
        }

        User.validateRawPassword(newPassword);

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        orderEmailService.sendPasswordChanged(user.getEmail(), user.getUsername());
    }

    @Transactional
    public void addBookToWishlist(String username, UUID bookId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));
        Book book = bookRepository.findByIdAndDeletedAtIsNull(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        user.addToWishlist(book);
        userRepository.save(user);
    }

    @Transactional
    public void removeBookFromWishlist(String username, UUID bookId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException("Книга не найдена"));

        user.removeFromWishlist(book);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<WishlistBookDto> getWishlist(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));
        return user.getWishlist().stream()
                .map(book -> WishlistBookDto.builder()
                        .id(book.getId())
                        .title(book.getTitle())
                        .cost(book.getCost())
                        .coverImageUrl(book.getCoverImage() != null ? book.getCoverImage().getUrl() : null)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Пользователь не найден"));

        List<AddressDto> addressDtos = user.getAddresses().stream()
                .map(addr -> AddressDto.builder()
                        .id(addr.getId())
                        .addressName(addr.getAddressName())
                        .addressText(addr.getAddressText())
                        .isDefault(addr.getIsDefault())
                        .build())
                .toList();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .contactPhone(user.getContactPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .addresses(addressDtos)
                .build();
    }
}