package com.bsuir.book_store.users.domain;

import com.bsuir.book_store.shared.exception.DomainException;
import com.bsuir.book_store.users.domain.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldRegisterUserWithValidData() {
        User user = User.register("testuser", "test@example.com", "encoded_pass", Role.CLIENT, "John", "Doe", "+375291112233");

        assertNull(user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("+375291112233", user.getContactPhone());
        assertEquals(Role.CLIENT, user.getRole());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void shouldDefaultToClientRoleWhenRoleIsNull() {
        User user = User.register("user1", "u@e.com", "p", null, null, null, null);

        assertEquals(Role.CLIENT, user.getRole());
    }

    @Test
    void shouldFailOnInvalidEmail() {
        assertThrows(DomainException.class, () -> User.register("user1", "invalid-email", "p", Role.CLIENT, null, null, null));
        assertThrows(DomainException.class, () -> User.register("user1", null, "p", Role.CLIENT, null, null, null));
        assertThrows(DomainException.class, () -> User.register("user1", "no-at-sign.com", "p", Role.CLIENT, null, null, null));
    }

    @Test
    void shouldFailOnInvalidUsername() {
        assertThrows(DomainException.class, () -> User.register("ab", "e@e.com", "p", Role.CLIENT, null, null, null)); // too short
        assertThrows(DomainException.class, () -> User.register("user with space", "e@e.com", "p", Role.CLIENT, null, null, null));
        assertThrows(DomainException.class, () -> User.register("тест", "e@e.com", "p", Role.CLIENT, null, null, null)); // cyrillic
    }

    @Test
    void shouldFailOnWeakPassword() {
        assertThrows(DomainException.class, () -> User.validateRawPassword("123"));
        assertThrows(DomainException.class, () -> User.validateRawPassword("onlyletters"));
        assertThrows(DomainException.class, () -> User.validateRawPassword("12345678")); // no letters
        assertThrows(DomainException.class, () -> User.validateRawPassword(null));
    }

    @Test
    void shouldAcceptStrongPassword() {
        assertDoesNotThrow(() -> User.validateRawPassword("StrongPass1"));
        assertDoesNotThrow(() -> User.validateRawPassword("abc12345"));
    }

    @Test
    void shouldIgnoreNullAndBlankProfileFields() {
        User user = User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, null);

        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getContactPhone());
    }

    @Test
    void updateProfileShouldSkipNullFields() {
        User user = User.register("user1", "e@e.com", "p", Role.CLIENT, "Old", "Name", null);
        user.updateProfile(null, "Updated", null);

        assertEquals("Old", user.getFirstName());
        assertEquals("Updated", user.getLastName());
    }

    @Test
    void shouldAddFirstAddressAsDefault() {
        User user = User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, null);
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(false).build();

        user.addAddress(addr);

        assertTrue(addr.getIsDefault());
        assertEquals(1, user.getAddresses().size());
    }

    @Test
    void shouldUnsetPreviousDefaultWhenNewDefaultAdded() {
        User user = User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, null);
        UserAddress addr1 = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(true).build();
        UserAddress addr2 = UserAddress.builder().addressName("Work").addressText("Street 2").isDefault(true).build();

        user.addAddress(addr1);
        user.addAddress(addr2);

        assertFalse(addr1.getIsDefault());
        assertTrue(addr2.getIsDefault());
        assertEquals(2, user.getAddresses().size());
    }

    @Test
    void shouldThrowWhenAddingNullAddress() {
        User user = User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, null);

        assertThrows(DomainException.class, () -> user.addAddress(null));
    }

    @Test
    void shouldFailOnInvalidPhone() {
        assertThrows(DomainException.class, () ->
                User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, "123") // too short
        );
        assertThrows(DomainException.class, () ->
                User.register("user1", "e@e.com", "p", Role.CLIENT, null, null, "not-a-phone")
        );
    }

    @Test
    void shouldChangePassword() {
        User user = User.register("user1", "e@e.com", "old_hash", Role.CLIENT, null, null, null);
        user.changePassword("new_hash");

        assertEquals("new_hash", user.getPassword());
    }

    @Test
    void changePasswordShouldFailOnBlank() {
        User user = User.register("user1", "e@e.com", "hash", Role.CLIENT, null, null, null);

        assertThrows(DomainException.class, () -> user.changePassword(null));
        assertThrows(DomainException.class, () -> user.changePassword("  "));
    }
}
