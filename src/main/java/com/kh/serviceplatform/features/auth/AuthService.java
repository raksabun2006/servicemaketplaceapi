package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.response.MessageResponse;
import com.kh.serviceplatform.features.auth.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
    AuthResponse login(LoginRequest request);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}
