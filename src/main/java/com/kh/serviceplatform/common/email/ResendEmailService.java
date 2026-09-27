package com.kh.serviceplatform.common.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendEmailService implements EmailService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final String mailFrom;
    private final String resendApiKey;
    private final String resendApiUrl;

    public ResendEmailService(
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${app.mail.from:}") String mailFrom,
            @Value("${app.mail.resend.api-key:}") String resendApiKey,
            @Value("${app.mail.resend.api-url:https://api.resend.com/emails}") String resendApiUrl
    ) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper().findAndRegisterModules();
        this.mailFrom = mailFrom != null ? mailFrom.trim() : "";
        this.resendApiKey = resendApiKey != null ? resendApiKey.trim() : "";
        this.resendApiUrl = (resendApiUrl != null && !resendApiUrl.isBlank()) ? resendApiUrl.trim() : "https://api.resend.com/emails";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) {
        sendPasswordResetEmail(email, "User", resetUrl, 15);
    }

    @Override
    public void sendPasswordResetEmail(String email, String recipientName, String resetUrl, int expirationMinutes) {
        if (email == null || email.isBlank()) {
            log.warn("Cannot send password reset email: recipient email is missing");
            return;
        }

        if (resendApiKey.isBlank()) {
            log.error("Resend API key is missing. Set RESEND_API_KEY environment variable to enable sending via Resend.");
            throw new IllegalStateException("Resend API key is missing");
        }

        String displayName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "there";
        String subject = "Reset your password";
        String textContent = buildPlainTextContent(displayName, resetUrl, expirationMinutes);
        String htmlContent = buildHtmlContent(displayName, resetUrl, expirationMinutes);

        // Security check: NEVER log reset tokens or complete reset URLs
        log.info("Dispatching password reset email to: {}", email);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", mailFrom);
            payload.put("to", List.of(email));
            payload.put("subject", subject);
            payload.put("html", htmlContent);
            payload.put("text", textContent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resendApiUrl))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Password reset email successfully sent via Resend to {}", email);
            } else {
                log.error("Resend API returned non-2xx status: {} for email: {}", response.statusCode(), email);
                throw new RuntimeException("Resend API returned error status: " + response.statusCode());
            }
        } catch (Exception ex) {
            log.error("Failed to send password reset email via Resend to {}: {}", email, ex.getMessage());
            throw new RuntimeException("Failed to send password reset email via Resend", ex);
        }
    }

    private String buildPlainTextContent(String name, String resetUrl, int expirationMinutes) {
        return """
                Password Reset

                We received a request to reset your password.

                Click the link below to create a new password:
                %s

                This link will expire after %d minutes.

                If you did not request a password reset, you can safely ignore this email.
                """.formatted(resetUrl, expirationMinutes);
    }

    private String buildHtmlContent(String name, String resetUrl, int expirationMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Reset your password</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 40px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width: 580px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);">
                          <tr>
                            <td style="background-color: #0f172a; padding: 32px 40px; text-align: center;">
                              <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;">Khmer Service</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding: 40px 40px 32px 40px;">
                              <h2 style="margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 600;">Password Reset</h2>
                              <p style="margin: 0 0 16px 0; color: #475569; font-size: 15px; line-height: 1.6;">We received a request to reset your password.</p>
                              <p style="margin: 0 0 28px 0; color: #475569; font-size: 15px; line-height: 1.6;">Click the button below to create a new password.</p>
                              <table role="presentation" cellspacing="0" cellpadding="0" style="margin: 0 auto 32px auto;">
                                <tr>
                                  <td align="center" style="border-radius: 8px; background-color: #2563eb;">
                                    <a href="%s" target="_blank" style="display: inline-block; padding: 14px 32px; font-size: 15px; font-weight: 600; color: #ffffff; text-decoration: none; border-radius: 8px;">Reset Password</a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin: 0 0 12px 0; color: #64748b; font-size: 14px; line-height: 1.5;">This link will expire after %d minutes.</p>
                              <p style="margin: 0 0 24px 0; color: #64748b; font-size: 14px; line-height: 1.5;">If you did not request a password reset, you can safely ignore this email.</p>
                              <div style="border-top: 1px solid #e2e8f0; padding-top: 20px;">
                                <p style="margin: 0; color: #94a3b8; font-size: 13px; line-height: 1.5;">For your security, this link can only be used once.</p>
                              </div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(resetUrl, expirationMinutes);
    }
}
