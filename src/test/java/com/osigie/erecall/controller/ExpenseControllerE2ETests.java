package com.osigie.erecall.controller;

import com.osigie.erecall.AbstractIntegrationTest;
import com.osigie.erecall.config.JwtService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
public class ExpenseControllerE2ETests extends AbstractIntegrationTest {

    private static final String EXPENSES_URL = "/api/v1/expenses";

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

    @BeforeEach
    void setUp() {
        expenseDocumentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getToken() {
        var user = userRepository.save(TestDataFactory.createTestUser());
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Nested
    @DisplayName("Submit expense")
    class SubmitTests {

        @Test
        void givenRawText_whenSubmit_thenReturn200() throws Exception {
            var token = getToken();
            var json = objectMapper.createObjectNode().put("rawText", "dinner 50");

            mockMvc.perform(post(EXPENSES_URL)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(json)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.documentId").exists())
                    .andExpect(jsonPath("$.data.status").value("PROCESSING"));
        }

        @Test
        void givenFileUrl_whenSubmit_thenReturn200() throws Exception {
            var token = getToken();
            var json = objectMapper.createObjectNode().put("fileUrl", "uploads/test.pdf");

            mockMvc.perform(post(EXPENSES_URL)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(json)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.documentId").exists())
                    .andExpect(jsonPath("$.data.status").value("PROCESSING"));
        }

        @Test
        void givenNoAuth_whenSubmit_thenReturn403() throws Exception {
            var json = objectMapper.createObjectNode().put("rawText", "dinner 50");

            mockMvc.perform(post(EXPENSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(json)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenEmptyBody_whenSubmit_thenReturn400() throws Exception {
            var token = getToken();

            mockMvc.perform(post(EXPENSES_URL)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get status")
    class StatusTests {

        @Test
        void givenValidId_whenGetStatus_thenReturn200() throws Exception {
            var token = getToken();
            var json = objectMapper.createObjectNode().put("rawText", "dinner 50");

            var response = mockMvc.perform(post(EXPENSES_URL)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(json)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var documentId = objectMapper.readTree(response)
                    .get("data").get("documentId").asString();

            mockMvc.perform(get(EXPENSES_URL + "/" + documentId + "/status")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.documentId").value(documentId));
        }

        @Test
        void givenNoAuth_whenGetStatus_thenReturn403() throws Exception {
            mockMvc.perform(get(EXPENSES_URL + "/" + UUID.randomUUID() + "/status"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenInvalidId_whenGetStatus_thenReturn404() throws Exception {
            var token = getToken();

            mockMvc.perform(get(EXPENSES_URL + "/" + UUID.randomUUID() + "/status")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }
}
