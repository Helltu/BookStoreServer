package com.bsuir.book_store.users.domain;

import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Should register user with valid data")
    void shouldRegisterUser() {
        User user = User.register(
                "testuser",
                "test@example.com",
                "encrypted_pass",
                Role.CLIENT,
                "John",
                "Doe",
                "+375291112233"
        );

        assertNull(user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw exception for invalid email")
    void shouldFailOnInvalidEmail() {
        assertThrows(DomainException.class, () -> User.register(
                "testuser",
                "invalid-email",
                "pass",
                Role.CLIENT,
                null, null, null
        ));
    }

    @Test
    @DisplayName("Should throw exception for short password")
    void shouldFailOnShortPassword() {
        assertThrows(DomainException.class, () -> User.validateRawPassword("123"));
    }

    @Test
    @DisplayName("Should add address and manage default flag")
    void shouldAddAddress() {
        User user = new User();

        user = User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, null);

        UserAddress addr1 = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(true).build();
        UserAddress addr2 = UserAddress.builder().addressName("Work").addressText("Street 2").isDefault(true).build();

        user.addAddress(addr1);
        assertTrue(addr1.getIsDefault());

        user.addAddress(addr2);

        assertFalse(addr1.getIsDefault());
        assertTrue(addr2.getIsDefault());
        assertEquals(2, user.getAddresses().size());
    }
}