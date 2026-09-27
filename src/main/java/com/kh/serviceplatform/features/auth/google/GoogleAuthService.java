package com.kh.serviceplatform.features.auth.google;


import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.GoogleLoginRequest;

public interface GoogleAuthService {

    /**
     * Authenticates a user using verified Google ID token credentials,
     * provisioning a CUSTOMER account if new, or authenticating existing user.
     *
     * @param request GoogleLoginRequest containing credential ID token
     * @return AuthResponse containing application JWT access & refresh tokens
     */
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
}
