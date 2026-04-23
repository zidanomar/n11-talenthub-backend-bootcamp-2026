package dev._am.n11_bootcamp.odev_2.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithAllFields() {
        User user = new User(1, "gandalf", "gofret");

        assertEquals(1, user.getId());
        assertEquals("gandalf", user.getUsername());
        assertEquals("gofret", user.getPassword());
    }
}
