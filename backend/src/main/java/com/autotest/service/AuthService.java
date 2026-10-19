package com.autotest.service;

import com.autotest.dto.AuthResponse;
import com.autotest.dto.LoginRequest;
import com.autotest.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
