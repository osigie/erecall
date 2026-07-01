package com.osigie.erecall.service;

import com.osigie.erecall.dto.AuthDTO;
import com.osigie.erecall.dto.AuthDTO.*;
import jakarta.validation.Valid;

public interface AuthService {
  AuthResponse register(@Valid RegisterRequest request);

  AuthResponse login(@Valid AuthDTO.LoginRequest request);
}
