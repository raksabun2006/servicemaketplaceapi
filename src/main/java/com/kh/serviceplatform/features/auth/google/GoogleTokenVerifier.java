package com.kh.serviceplatform.features.auth.google;

public interface GoogleTokenVerifier {

    /**
     * Verifies the Google ID token signature, issuer, audience, and expiration,
     * and extracts verified user identity claims.
     *
     * @param idTokenString Google ID token credential
     * @return GoogleUserInfo containing verified claims
     */
    GoogleUserInfo verifyToken(String idTokenString);
}
