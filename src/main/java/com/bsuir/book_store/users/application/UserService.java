package com.bsuir.book_store.users.application;

import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.api.dto.AddAddressRequest;
import com.bsuir.book_store.users.api.dto.AddressDto;
import com.bsuir.book_store.users.api.dto.UpdateProfileRequest;
import com.bsuir.book_store.users.api.dto.UserProfileResponse;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.UserAddress;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        user.updateProfile(request.getFirstName(), request.getLastName(), request.getPhone());

        userRepository.save(user);
    }

    @Transactional
    public void addAddress(String username, AddAddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        UserAddress address = UserAddress.builder()
                .addressName(request.getAddressName())
                .addressText(request.getAddressText())
                .isDefault(request.getIsDefault())
                .build();

        user.addAddress(address);

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

        User.validateRawPassword(newPassword);

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found"));

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