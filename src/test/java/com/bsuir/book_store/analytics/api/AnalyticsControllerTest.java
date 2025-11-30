package com.bsuir.book_store.analytics.api;

import com.bsuir.book_store.catalog.domain.model.Book;
import com.bsuir.book_store.catalog.infrastructure.BookRepository;
import com.bsuir.book_store.orders.domain.Order;
import com.bsuir.book_store.orders.infrastructure.OrderRepository;
import com.bsuir.book_store.users.domain.User;
import com.bsuir.book_store.users.domain.enums.Role;
import com.bsuir.book_store.users.infrastructure.UserRepository;
import com.bsuir.book_store.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean
    private BookElasticRepository elasticRepository;

    private String managerToken;
    private String clientToken;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        bookRepository.deleteAll();

        User manager = User.register("admin", "admin@test.com", passwordEncoder.encode("p"), Role.MANAGER, null, null, null);
        userRepository.save(manager);
        managerToken = jwtService.generateToken(manager);

        User client = User.register("client", "c@test.com", passwordEncoder.encode("p"), Role.CLIENT, null, null, null);
        userRepository.save(client);
        clientToken = jwtService.generateToken(client);

        Book book = Book.builder().title("Java Book").isbn("1").cost(new BigDecimal("100.00")).stockQuantity(10).build();
        bookRepository.save(book);

        Order order = Order.create(client);
        order.addItem(book, 2);
        orderRepository.save(order);
    }

    @Test
    void managerShouldSeeDashboard() throws Exception {
        mockMvc.perform(get("/api/analytics")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalOrders").value(1))
                .andExpect(jsonPath("$.summary.totalRevenue").value(200.0))
                .andExpect(jsonPath("$.summary.totalBooksSold").value(2));
    }

    @Test
    void clientShouldNotSeeDashboard() throws Exception {
        mockMvc.perform(get("/api/analytics")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}