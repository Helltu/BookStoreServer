package com.bsuir.book_store.orders.api;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.api.dto.CreateOrderRequest;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.enums.Role;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import com.bsuir.book_store.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookElasticRepository elasticRepository;

    private String token;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        userRepository.deleteAll();
        bookRepository.deleteAll();

        User user = User.register("client", "client@test.com", passwordEncoder.encode("pass"), Role.CLIENT, null, null, null);
        userRepository.save(user);
        token = jwtService.generateToken(user);

        Book book = Book.builder()
                .title("Test Book")
                .isbn("1234567890")
                .cost(BigDecimal.TEN)
                .stockQuantity(10)
                .build();
        bookId = bookRepository.save(book).getId();
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();

        CreateOrderRequest.ItemDto item = new CreateOrderRequest.ItemDto();
        item.setBookId(bookId);
        item.setQuantity(1);
        request.setItems(List.of(item));

        CreateOrderRequest.DeliveryDto delivery = new CreateOrderRequest.DeliveryDto();
        delivery.setAddress("Minsk");
        delivery.setPhone("+375000000000");
        delivery.setCustomerName("Ivan");
        request.setDeliveryDetails(delivery);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}