package com.kh.serviceplatform.features.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.password-reset.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class PasswordResetCleanupScheduler {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "${security.password-reset.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredAndUsedTokens() {
        try {
            Instant now = Instant.now();
            int deletedCount = passwordResetTokenRepository.deleteAllExpiredOrUsedBefore(now);
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired or used password reset tokens", deletedCount);
            }
        } catch (Exception ex) {
            log.error("Failed to clean up expired password reset tokens: {}", ex.getMessage());
        }
    }
}
