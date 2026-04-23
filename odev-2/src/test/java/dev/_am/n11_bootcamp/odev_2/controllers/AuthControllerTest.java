package dev._am.n11_bootcamp.odev_2.controllers;

import dev._am.n11_bootcamp.odev_2.config.GlobalExceptionHandler;
import dev._am.n11_bootcamp.odev_2.dto.SignupRequest;
import dev._am.n11_bootcamp.odev_2.repositories.impl.UserRepositoryImpl;
import dev._am.n11_bootcamp.odev_2.services.AuthService;
import dev._am.n11_bootcamp.odev_2.services.JwtService;
import dev._am.n11_bootcamp.odev_2.services.impl.AuthServiceImpl;
import dev._am.n11_bootcamp.odev_2.services.impl.JwtServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SECRET = "c2VjcmV0LWtleS1mb3ItdGVzdC1wdXJwb3Nlcy1vbmx5LTMyY2hhcg==";

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 1L, 3L);
        AuthService authService = new AuthServiceImpl(
                new UserRepositoryImpl(),
                new BCryptPasswordEncoder(4),
                jwtService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, jwtService))
                .setValidator(new LocalValidatorFactoryBean())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void signup_shouldReturn200WithAccessTokenAndSetRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "mordor123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshExpiresAt").isNumber())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void signin_shouldReturn200WithAccessTokenAndSetRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "mordor123"))));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "mordor123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshExpiresAt").isNumber())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void refresh_shouldReturn200WithNewAccessToken() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "mordor123"))))
                .andReturn();

        Cookie refreshCookie = signupResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void refresh_shouldReturn400WhenNoCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_shouldClearRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    void signup_shouldReturn400WhenUsernameExists() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "mordor123"))));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "mordor123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void signup_shouldReturn400WhenUsernameIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("", "mordor123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void signup_shouldReturn400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("sauron", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
