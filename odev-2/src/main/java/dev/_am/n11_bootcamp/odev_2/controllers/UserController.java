package dev._am.n11_bootcamp.odev_2.controllers;

import dev._am.n11_bootcamp.odev_2.domain.User;
import dev._am.n11_bootcamp.odev_2.dto.ApiResponse;
import dev._am.n11_bootcamp.odev_2.dto.UserResponse;
import dev._am.n11_bootcamp.odev_2.services.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<User>> getUsers() {
        return new ApiResponse<>(userService.getAll());
    }

    @GetMapping("/current")
    public ApiResponse<UserResponse> getCurrentUser() {
        int userId = (int) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.getById(userId);
        return new ApiResponse<>(new UserResponse(user.getUsername()));
    }
}
