package com.kh.serviceplatform.features.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetCleanupSchedulerTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetCleanupScheduler cleanupScheduler;

    @Test
    void shouldCallDeleteAllExpiredOrUsedBeforeOnCleanup() {
        when(passwordResetTokenRepository.deleteAllExpiredOrUsedBefore(any(Instant.class))).thenReturn(5);

        cleanupScheduler.cleanupExpiredAndUsedTokens();

        verify(passwordResetTokenRepository).deleteAllExpiredOrUsedBefore(any(Instant.class));
    }
}
