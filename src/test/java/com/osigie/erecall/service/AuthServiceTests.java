package com.osigie.erecall.service;

import com.osigie.erecall.config.JwtService;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.AuthDTO.*;
import com.osigie.erecall.exception.BadRequestException;
import com.osigie.erecall.exception.UnauthorizedException;
import com.osigie.erecall.repo.UserRepository;
import com.osigie.erecall.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_withValidRequest_shouldReturnAuthResponse() {
        var request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setUsername("testuser");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        when(jwtService.generateAccessToken(any(), eq("test@example.com"), eq("USER"))).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpiry()).thenReturn(3600000L);

        var response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600000L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("USER");
        assertThat(response.getUser().getId()).isNotNull();

        verify(userRepository).save(any());
    }

    @Test
    void register_withExistingEmail_shouldThrowBadRequest() {
        var request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("Password123!");
        request.setUsername("testuser");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_shouldReturnAuthResponse() {
        var request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        var user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .username("testuser")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user.getId(), "test@example.com", "USER")).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpiry()).thenReturn(3600000L);

        var response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("USER");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_withBadCredentials_shouldPropagateBadCredentialsException() {
        var request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("bad credentials");

        verify(userRepository, never()).findByEmail(any());
    }
}
