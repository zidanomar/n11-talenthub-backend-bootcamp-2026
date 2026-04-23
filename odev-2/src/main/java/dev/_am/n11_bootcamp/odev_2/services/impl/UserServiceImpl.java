package dev._am.n11_bootcamp.odev_2.services.impl;

import dev._am.n11_bootcamp.odev_2.domain.User;
import dev._am.n11_bootcamp.odev_2.repositories.UserRepository;
import dev._am.n11_bootcamp.odev_2.services.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public User getById(int id) {
        return userRepository.findById(id);
    }

    @Override
    public User create(User user) {
        return userRepository.save(user);
    }
}
