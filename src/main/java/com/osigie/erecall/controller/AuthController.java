package com.osigie.erecall.controller;

import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.AuthDTO.*;
import com.osigie.erecall.dto.BaseResponse;
import com.osigie.erecall.security.AuthHelper;
import com.osigie.erecall.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthHelper authHelper;

    public AuthController(AuthService authService, AuthHelper authHelper) {
        this.authService = authService;
        this.authHelper = authHelper;
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return new ResponseEntity<>(BaseResponse.success(authResponse), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(BaseResponse.success(authResponse));
    }


    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserInfo>> me() {
        User user = authHelper.getAuthenticatedUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole().name());
        return ResponseEntity.ok(BaseResponse.success(userInfo));
    }

}