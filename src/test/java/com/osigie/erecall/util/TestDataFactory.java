package com.osigie.erecall.util;

import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.AuthDTO.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestDataFactory {

    public static final String DEFAULT_TEST_EMAIL = "test@example.com";
    public static final String DEFAULT_TEST_PASSWORD = "Password123!";

    public static RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("osigie");
        request.setPassword("Osigie@88");
        request.setEmail("kenosagie88@gmail.com");
        return request;
    }


    public static User createTestUser() {
        var encoder = new BCryptPasswordEncoder();
        var user = User.builder()
                .email(DEFAULT_TEST_EMAIL)
                .username("testuser")
                .passwordHash(encoder.encode(DEFAULT_TEST_PASSWORD))
                .role(User.Role.USER)
                .build();
        user.setEmailVerified(true);
        return user;
    }
}
