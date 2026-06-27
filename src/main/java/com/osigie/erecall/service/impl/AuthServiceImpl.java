package com.osigie.erecall.service.impl;

import com.osigie.erecall.config.JwtService;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.AuthDTO.*;
import com.osigie.erecall.exception.BadRequestException;
import com.osigie.erecall.exception.UnauthorizedException;
import com.osigie.erecall.repo.UserRepository;
import com.osigie.erecall.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    private UserInfo createUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId().toString());
        info.setEmail(user.getEmail());
        info.setRole(user.getRole().name());
        return info;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse(
                accessToken,
                jwtService.getAccessTokenExpiry()
        );
        response.setUser(createUserInfo(user));
        return response;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse(
                accessToken,
                jwtService.getAccessTokenExpiry()
        );
        response.setUser(createUserInfo(user));
        return response;
    }


}
