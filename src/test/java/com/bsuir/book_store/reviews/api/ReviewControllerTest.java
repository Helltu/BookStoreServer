package com.bsuir.book_store.reviews.api;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.reviews.api.dto.CreateReviewRequest;
import com.bsuir.book_store.reviews.infrastructure.ReviewRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository elasticRepository;

    private String clientToken;
    private String managerToken;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        userRepository.deleteAll();
        bookRepository.deleteAll();

        User client = User.register("client", "c@test.com", passwordEncoder.encode("123"), Role.CLIENT, null, null, null);
        userRepository.save(client);
        clientToken = jwtService.generateToken(client);

        User manager = User.register("manager", "m@test.com", passwordEncoder.encode("123"), Role.MANAGER, null, null, null);
        userRepository.save(manager);
        managerToken = jwtService.generateToken(manager);

        Book book = Book.builder().title("Test Book").isbn("111").cost(BigDecimal.TEN).stockQuantity(10).build();
        bookId = bookRepository.save(book).getId();
    }

    @Test
    void shouldAddReviewSuccessfully() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookId(bookId);
        request.setRating(5);
        request.setText("Excellent!");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldForbidDuplicateReview() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookId(bookId);
        request.setRating(5);
        request.setText("First");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerShouldDeleteReview() throws Exception {
        User client = userRepository.findByUsername("client").get();
        Book book = bookRepository.findById(bookId).get();
        var review = com.bsuir.book_store.reviews.domain.Review.leave(client, book, 5, "Text");
        UUID reviewId = reviewRepository.save(review).getId();

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void clientCannotDeleteReview() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(delete("/api/reviews/" + randomId)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}