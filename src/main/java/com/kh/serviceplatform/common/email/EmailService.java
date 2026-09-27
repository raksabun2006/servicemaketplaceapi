package com.kh.serviceplatform.common.email;

public interface EmailService {

    void sendPasswordResetEmail(String email, String resetUrl);

    void sendPasswordResetEmail(String email, String recipientName, String resetUrl, int expirationMinutes);
}
