package dev._am.n11_bootcamp.odev_2.controllers;

import dev._am.n11_bootcamp.odev_2.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getUsers_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_shouldReturn200WithValidToken() throws Exception {
        String token = jwtService.generateToken(1);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].username").value("gandalf"));
    }

    @Test
    void getCurrentUser_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_shouldReturnUsernameWithValidToken() throws Exception {
        String token = jwtService.generateToken(1);

        mockMvc.perform(get("/api/users/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("gandalf"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
}
