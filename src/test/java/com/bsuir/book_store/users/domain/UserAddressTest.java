package com.bsuir.book_store.users.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAddressTest {

    @Test
    void makeDefaultShouldSetIsDefaultTrue() {
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(false).build();
        addr.makeDefault();
        assertTrue(addr.getIsDefault());
    }

    @Test
    void unsetDefaultShouldSetIsDefaultFalse() {
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(true).build();
        addr.unsetDefault();
        assertFalse(addr.getIsDefault());
    }

    @Test
    void updateShouldChangeProvidedFields() {
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Old Street").isDefault(false).build();
        addr.update("Work", "New Street", true);

        assertEquals("Work", addr.getAddressName());
        assertEquals("New Street", addr.getAddressText());
        assertTrue(addr.getIsDefault());
    }

    @Test
    void updateShouldSkipNullFields() {
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Old Street").isDefault(true).build();
        addr.update(null, null, null);

        assertEquals("Home", addr.getAddressName());
        assertEquals("Old Street", addr.getAddressText());
        assertTrue(addr.getIsDefault());
    }

    @Test
    void updateShouldChangeOnlyName() {
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(false).build();
        addr.update("Office", null, null);

        assertEquals("Office", addr.getAddressName());
        assertEquals("Street 1", addr.getAddressText());
        assertFalse(addr.getIsDefault());
    }

    @Test
    void assignToUserShouldSetUser() {
        User user = new User();
        UserAddress addr = UserAddress.builder().addressName("Home").addressText("Street 1").isDefault(false).build();
        addr.assignToUser(user);

        assertSame(user, addr.getUser());
    }
}
