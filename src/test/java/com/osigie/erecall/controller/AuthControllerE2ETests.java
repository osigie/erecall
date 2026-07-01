package com.osigie.erecall.controller;

import com.osigie.erecall.AbstractIntegrationTest;
import com.osigie.erecall.config.JwtService;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.AuthDTO.*;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.UserRepository;
import com.osigie.erecall.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthControllerE2ETests extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/auth/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseDocumentRepository expenseDocumentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        expenseDocumentRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(TestDataFactory.createTestUser());
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        void givenValidRequest_whenRegister_thenReturn201() throws Exception {
            var request = TestDataFactory.createRegisterRequest();

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.user.email").value(request.getEmail()));
        }

        @Test
        void givenExistingEmail_whenRegister_thenReturn400() throws Exception {
            var request = new RegisterRequest();
            request.setEmail(TestDataFactory.DEFAULT_TEST_EMAIL);
            request.setUsername("newuser");
            request.setPassword("NewPass123!");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenInvalidEmail_whenRegister_thenReturn400() throws Exception {
            var request = new RegisterRequest();
            request.setEmail("not-an-email");
            request.setUsername("newuser");
            request.setPassword("NewPass123!");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenShortPassword_whenRegister_thenReturn400() throws Exception {
            var request = new RegisterRequest();
            request.setEmail("short@example.com");
            request.setUsername("newuser");
            request.setPassword("Ab1!");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenMissingFields_whenRegister_thenReturn400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        void givenValidCredentials_whenLogin_thenReturn200() throws Exception {
            var loginRequest = new LoginRequest();
            loginRequest.setEmail(TestDataFactory.DEFAULT_TEST_EMAIL);
            loginRequest.setPassword(TestDataFactory.DEFAULT_TEST_PASSWORD);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.user.email").value(TestDataFactory.DEFAULT_TEST_EMAIL));
        }

        @Test
        void givenWrongPassword_whenLogin_thenReturn401() throws Exception {
            var loginRequest = new LoginRequest();
            loginRequest.setEmail(TestDataFactory.DEFAULT_TEST_EMAIL);
            loginRequest.setPassword("WrongPassword1!");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNonExistentEmail_whenLogin_thenReturn401() throws Exception {
            var loginRequest = new LoginRequest();
            loginRequest.setEmail("nonexistent@example.com");
            loginRequest.setPassword(TestDataFactory.DEFAULT_TEST_PASSWORD);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Me")
    class MeTests {

        @Test
        void givenAuthenticated_whenGetMe_thenReturn200() throws Exception {
            var token = jwtService.generateAccessToken(
                    testUser.getId(), testUser.getEmail(), testUser.getRole().name());

            mockMvc.perform(get(ME_URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(TestDataFactory.DEFAULT_TEST_EMAIL));
        }

        @Test
        void givenNoAuth_whenGetMe_thenReturn403() throws Exception {
            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isForbidden());
        }
    }
}
