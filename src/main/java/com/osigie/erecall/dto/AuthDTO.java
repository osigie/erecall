package com.osigie.erecall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

  @Data
  public static class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$",
        message =
            "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;
  }

  @Data
  public static class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
  }

  @Data
  public static class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;

    public AuthResponse(String accessToken, long expiresIn) {
      this.accessToken = accessToken;
      this.tokenType = "Bearer";
      this.expiresIn = expiresIn;
    }
  }

  @Data
  @NoArgsConstructor
  public static class UserInfo {
    private String id;
    private String email;
    private String role;
  }
}
