package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.service.AuthService;
import com.subscriptiontracker.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private com.subscriptiontracker.repository.UserRepository userRepository;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .baseCurrencyCode("USD")
                .createdAt(LocalDateTime.now())
                .build();

        authResponse = AuthResponse.builder()
                .user(userResponse)
                .token("jwt-token-value")
                .build();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 when registration is successful")
        void shouldReturn201WhenRegistrationIsSuccessful() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.user.baseCurrencyCode").value("USD"))
                    .andExpect(jsonPath("$.token").value("jwt-token-value"));
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("invalid-email")
                    .password("Password123")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("Pass1")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when password lacks uppercase letter")
        void shouldReturn400WhenPasswordLacksUppercase() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new BadRequestException("Registration failed. Please try again."));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("should return 400 when email is empty")
        void shouldReturn400WhenEmailIsEmpty() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("")
                    .password("Password123")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 with token when credentials are valid")
        void shouldReturn200WithTokenWhenCredentialsAreValid() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.token").value("jwt-token-value"));
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("should return 400 when email is empty")
        void shouldReturn400WhenEmailIsEmpty() throws Exception {
            LoginRequest invalidRequest = LoginRequest.builder()
                    .email("")
                    .password("Password123")
                    .build();

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is empty")
        void shouldReturn400WhenPasswordIsEmpty() throws Exception {
            LoginRequest invalidRequest = LoginRequest.builder()
                    .email("test@example.com")
                    .password("")
                    .build();

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("should return 204 for logout")
        void shouldReturn204ForLogout() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /auth/health")
    class Health {

        @Test
        @DisplayName("should return 200 for health check")
        void shouldReturn200ForHealthCheck() throws Exception {
            mockMvc.perform(get("/auth/health"))
                    .andExpect(status().isOk());
        }
    }
}
