package com.bsuir.book_store.assistant.api;

import com.bsuir.book_store.assistant.api.dto.ChatRequest;
import com.bsuir.book_store.assistant.application.BookStoreAssistant;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean
    private BookStoreAssistant assistant;

    @MockitoBean
    private com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository elasticRepository;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = User.register("user", "u@u.com", passwordEncoder.encode("p"), Role.CLIENT, null, null, null);
        userRepository.save(user);
        token = jwtService.generateToken(user);
    }

    @Test
    void shouldReturnAiResponse() throws Exception {
        when(assistant.chat(eq("user"), anyString())).thenReturn("I am AI");

        ChatRequest request = new ChatRequest();
        request.setMessage("Hello!");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("I am AI"));
    }

    @Test
    void shouldFailWithoutAuth() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello!");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}