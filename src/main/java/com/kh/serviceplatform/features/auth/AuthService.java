package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.GoogleLoginRequest;
import com.kh.serviceplatform.features.auth.dto.LoginRequest;
import com.kh.serviceplatform.features.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
    AuthResponse login(LoginRequest request);
}
