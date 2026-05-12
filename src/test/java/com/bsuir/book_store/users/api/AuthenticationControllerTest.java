package com.bsuir.book_store.users.api;

import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import com.bsuir.book_store.recommendations.infrastructure.BookCoOccurrenceRepository;
import com.bsuir.book_store.users.api.dto.RegisterRequest;
import com.bsuir.book_store.users.application.EmailVerificationService;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.enums.Role;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import com.bsuir.book_store.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @MockitoBean private BookElasticRepository bookElasticRepository;
    @MockitoBean private BookCoOccurrenceRepository bookCoOccurrenceRepository;
    @MockitoBean private EmailVerificationService emailVerificationService;
    @MockitoBean private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailVerificationService).initiate(any());
    }

    @Test
    void registerShouldInitiateVerificationAndReturn2xx() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("StrongPass1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void registerShouldFailWithWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWithDuplicateUsername() throws Exception {
        userRepository.save(User.register("existinguser", "e@test.com", passwordEncoder.encode("pass"), Role.CLIENT, null, null, null));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@test.com");
        request.setPassword("StrongPass1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWithDuplicateEmail() throws Exception {
        userRepository.save(User.register("user1", "taken@test.com", passwordEncoder.encode("pass"), Role.CLIENT, null, null, null));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser2");
        request.setEmail("taken@test.com");
        request.setPassword("StrongPass1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() throws Exception {
        userRepository.save(User.register("loginuser", "login@test.com", passwordEncoder.encode("ValidPass1"), Role.CLIENT, null, null, null));

        String body = objectMapper.writeValueAsString(new Object() {
            public final String username = "loginuser";
            public final String password = "ValidPass1";
        });

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void loginShouldFailForWrongPassword() throws Exception {
        userRepository.save(User.register("loginuser2", "l2@test.com", passwordEncoder.encode("CorrectPass1"), Role.CLIENT, null, null, null));

        String body = objectMapper.writeValueAsString(new Object() {
            public final String username = "loginuser2";
            public final String password = "WrongPass999";
        });

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}
