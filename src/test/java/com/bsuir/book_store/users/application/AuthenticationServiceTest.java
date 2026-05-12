package com.bsuir.book_store.users.application;

import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.security.JwtService;
import com.bsuir.book_store.users.api.dto.AuthenticationRequest;
import com.bsuir.book_store.users.api.dto.AuthenticationResponse;
import com.bsuir.book_store.users.api.dto.RegisterRequest;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.enums.Role;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("newuser1");
        validRequest.setEmail("new@test.com");
        validRequest.setPassword("StrongPass1");
    }

    @Test
    void registerShouldInitiateVerification() {
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        doNothing().when(emailVerificationService).initiate(any());

        authenticationService.register(validRequest);

        verify(emailVerificationService).initiate(validRequest);
    }

    @Test
    void registerShouldThrowOnDuplicateUsername() {
        User existing = User.register("newuser1", "other@test.com", "h", Role.CLIENT, null, null, null);
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.of(existing));

        assertThrows(DomainException.class, () -> authenticationService.register(validRequest));
        verify(emailVerificationService, never()).initiate(any());
    }

    @Test
    void registerShouldThrowOnDuplicateEmail() {
        User existing = User.register("otheruser", "new@test.com", "h", Role.CLIENT, null, null, null);
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(existing));

        assertThrows(DomainException.class, () -> authenticationService.register(validRequest));
        verify(emailVerificationService, never()).initiate(any());
    }

    @Test
    void registerShouldThrowOnWeakPassword() {
        validRequest.setPassword("weak");
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> authenticationService.register(validRequest));
        verify(emailVerificationService, never()).initiate(any());
    }

    @Test
    void completeRegistrationShouldSaveUserAndReturnToken() {
        when(emailVerificationService.verify("new@test.com", "123456")).thenReturn(validRequest);
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.completeRegistration("new@test.com", "123456");

        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void completeRegistrationShouldThrowIfUsernameAlreadyTaken() {
        when(emailVerificationService.verify("new@test.com", "123456")).thenReturn(validRequest);
        User existing = User.register("newuser1", "other@test.com", "h", Role.CLIENT, null, null, null);
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.of(existing));

        assertThrows(DomainException.class,
                () -> authenticationService.completeRegistration("new@test.com", "123456"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateShouldReturnToken() {
        User user = User.register("newuser1", "new@test.com", "hashed", Role.CLIENT, null, null, null);
        when(userRepository.findByUsername("newuser1")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("newuser1");
        request.setPassword("StrongPass1");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertEquals("jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateShouldThrowOnBadCredentials() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("newuser1");
        request.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));
    }

    @Test
    void authenticateShouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("ghost");
        request.setPassword("pass");

        assertThrows(DomainException.class, () -> authenticationService.authenticate(request));
    }
}
