package dev._am.n11_bootcamp.odev_2.repositories.impl;

import dev._am.n11_bootcamp.odev_2.domain.User;
import dev._am.n11_bootcamp.odev_2.repositories.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final List<User> users = new ArrayList<>(List.of(
            new User(1, "gandalf", "gofret123"),
            new User(2, "frodo", "shire123"),
            new User(3, "bilbo", "baggins1"),
            new User(4, "aragorn", "strider1"),
            new User(5, "legolas", "greenleaf1")
    ));

    @Override
    public List<User> findAll() {
        return users;
    }

    @Override
    public User findById(int id) {
        return users.stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public User findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Override
    public User save(User user) {
        int nextId = users.stream().mapToInt(User::getId).max().orElse(0) + 1;
        User saved = new User(nextId, user.getUsername(), user.getPassword());
        users.add(saved);
        return saved;
    }
}
