package dev._am.n11_bootcamp.odev_2.services;

import dev._am.n11_bootcamp.odev_2.domain.User;
import dev._am.n11_bootcamp.odev_2.repositories.UserRepository;
import dev._am.n11_bootcamp.odev_2.repositories.impl.UserRepositoryImpl;
import dev._am.n11_bootcamp.odev_2.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceImplTest {

    private UserService userService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl();
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void getAll_shouldReturnAllSeededUsers() {
        List<User> users = userService.getAll();

        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    void getById_shouldReturnCorrectUser() {
        User user = userService.getById(1);

        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("gandalf", user.getUsername());
    }

    @Test
    void getById_shouldReturnNullWhenNotFound() {
        User user = userService.getById(999);

        assertNull(user);
    }

    @Test
    void create_shouldSaveAndReturnUserWithGeneratedId() {
        User newUser = new User("sauron", "mordor123");

        User created = userService.create(newUser);

        assertNotNull(created);
        assertTrue(created.getId() > 0);
        assertEquals("sauron", created.getUsername());
        assertNotNull(userRepository.findByUsername("sauron"));
    }
}
