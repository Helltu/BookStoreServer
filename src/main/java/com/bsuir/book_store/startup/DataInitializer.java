package com.bsuir.book_store.startup;

import com.bsuir.book_store.entity.Role;
import com.bsuir.book_store.entity.User;
import com.bsuir.book_store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${manager.password}")
    private String managerPassword;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByRole(Role.MANAGER).isEmpty()) {
            log.info("No managers found. Creating a default manager account...");
            User manager = User.builder()
                    .username("manager")
                    .email("manager@bookstore.com")
                    .password(passwordEncoder.encode(managerPassword))
                    .role(Role.MANAGER)
                    .build();
            userRepository.save(manager);
            log.info("Default manager account created with username 'manager'");
        }
    }
}