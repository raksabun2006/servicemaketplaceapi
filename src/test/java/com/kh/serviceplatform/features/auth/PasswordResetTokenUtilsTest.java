package com.kh.serviceplatform.features.auth;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenUtilsTest {

    @Test
    void shouldGenerateCryptographicallySecureUniqueTokens() {
        Set<String> generatedTokens = new HashSet<>();
        int count = 100;

        for (int i = 0; i < count; i++) {
            String token = PasswordResetTokenUtils.generateRawToken();
            assertNotNull(token);
            // 32 bytes encoded with URL-safe Base64 without padding = 43 characters
            assertEquals(43, token.length());
            assertFalse(token.contains("="), "Token should not contain padding characters");
            assertFalse(token.contains("+"), "Token should be URL-safe");
            assertFalse(token.contains("/"), "Token should be URL-safe");
            generatedTokens.add(token);
        }

        // All 100 generated tokens must be completely unique
        assertEquals(count, generatedTokens.size());
    }

    @Test
    void shouldComputeConsistentSha256HexHash() {
        String token = "sample-secure-token-1234567890";
        String hash1 = PasswordResetTokenUtils.hashToken(token);
        String hash2 = PasswordResetTokenUtils.hashToken(token);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex string must be exactly 64 characters");
        assertEquals(hash1, hash2, "Hashing must be deterministic");
        assertTrue(hash1.matches("^[0-9a-f]{64}$"), "Hash must be lowercase hexadecimal");
    }

    @Test
    void shouldReturnNullWhenHashingNullToken() {
        assertNull(PasswordResetTokenUtils.hashToken(null));
    }
}
