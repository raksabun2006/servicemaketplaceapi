package com.kh.serviceplatform.features.auth.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private final String configuredClientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierImpl(
            @Value("${google.client-id:}") String configuredClientId) {
        this.configuredClientId = configuredClientId != null ? configuredClientId.trim() : "";
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        );
        if (!this.configuredClientId.isBlank()) {
            builder.setAudience(Collections.singletonList(this.configuredClientId));
        }
        this.verifier = builder.build();
    }

    @Override
    public GoogleUserInfo verifyToken(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BadRequestException("Google credential token is required");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString.trim());
        } catch (Exception e) {
            log.warn("Google ID token verification failed with exception: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Google ID token");
        }

        if (idToken == null) {
            log.warn("Google ID token verification returned null (signature, audience, issuer, or expiration mismatch)");
            throw new UnauthorizedException("Invalid or expired Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // 1. Validate email_verified
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            log.warn("Google token rejected: email is not verified for sub: {}", payload.getSubject());
            throw new UnauthorizedException("Google account email is not verified");
        }

        String subject = payload.getSubject();
        String email = payload.getEmail();

        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            log.warn("Google token rejected: missing subject or email in token claims");
            throw new UnauthorizedException("Invalid Google ID token claims");
        }

        // 2. Validate audience against configuredClientId if configured
        if (!this.configuredClientId.isBlank()) {
            Object audObj = payload.getAudience();
            boolean audienceMatched = false;
            if (audObj instanceof String audStr) {
                audienceMatched = this.configuredClientId.equals(audStr);
            } else if (audObj instanceof List<?> audList) {
                audienceMatched = audList.contains(this.configuredClientId);
            }
            if (!audienceMatched) {
                log.warn("Google token audience '{}' does not match configured client id", audObj);
                throw new UnauthorizedException("Google ID token audience mismatch");
            }
        }

        // 3. Validate issuer
        String issuer = payload.getIssuer();
        if (issuer == null || (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer))) {
            log.warn("Google token issuer '{}' is not valid", issuer);
            throw new UnauthorizedException("Invalid Google ID token issuer");
        }

        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");

        return GoogleUserInfo.builder()
                .subject(subject)
                .email(email.toLowerCase().trim())
                .emailVerified(true)
                .name(name)
                .picture(picture)
                .givenName(givenName)
                .familyName(familyName)
                .build();
    }
}
