package com.kh.serviceplatform.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GoogleLoginRequest(
        @JsonAlias({"idToken", "token"})
        String credential
) {
    public String resolveCredential() {
        return credential != null ? credential.trim() : null;
    }
}
