package com.bsuir.book_store.users.application;

import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.shared.security.JwtService;
import com.bsuir.book_store.users.api.dto.AuthenticationRequest;
import com.bsuir.book_store.users.api.dto.AuthenticationResponse;
import com.bsuir.book_store.users.api.dto.RegisterRequest;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.enums.Role;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DomainException("Пользователь с таким логином уже существует");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DomainException("Пользователь с таким email уже существует");
        }

        User.validateRawPassword(request.getPassword());

        User user = User.register(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Role.CLIENT,
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()
        );

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new DomainException("Пользователь не найден"));
        return AuthenticationResponse.builder().token(jwtService.generateToken(user)).build();
    }
}