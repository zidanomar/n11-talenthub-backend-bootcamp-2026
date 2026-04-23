package dev._am.n11_bootcamp.odev_2.repositories;

import dev._am.n11_bootcamp.odev_2.domain.User;
import dev._am.n11_bootcamp.odev_2.repositories.impl.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryImplTest {

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl();
    }

    @Test
    void findAll_shouldReturnListOfUsers() {
        List<User> users = userRepository.findAll();

        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    void findById_shouldReturnCorrectUser() {
        User user = userRepository.findById(1);

        assertNotNull(user);
        assertEquals(1, user.getId());
    }

    @Test
    void findById_shouldReturnNullWhenNotFound() {
        User user = userRepository.findById(999);

        assertNull(user);
    }

    @Test
    void findByUsername_shouldReturnCorrectUser() {
        User user = userRepository.findByUsername("gandalf");

        assertNotNull(user);
        assertEquals("gandalf", user.getUsername());
    }

    @Test
    void findByUsername_shouldReturnNullWhenNotFound() {
        User user = userRepository.findByUsername("sauron");

        assertNull(user);
    }

    @Test
    void save_shouldAddUserToList() {
        userRepository.save(new User("bilbo2", "baggins"));

        assertEquals(6, userRepository.findAll().size());
    }
}
