package dev._am.n11_bootcamp.odev_2.services;

import dev._am.n11_bootcamp.odev_2.domain.User;

import java.util.List;

public interface UserService {

    List<User> getAll();
    User getById(int id);
    User create(User user);
}
