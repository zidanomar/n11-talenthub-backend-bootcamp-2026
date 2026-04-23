package dev._am.n11_bootcamp.odev_2.repositories;

import dev._am.n11_bootcamp.odev_2.domain.User;

import java.util.List;

public interface UserRepository {

    List<User> findAll();
    User findById(int id);
    User findByUsername(String username);
    User save(User user);
}
