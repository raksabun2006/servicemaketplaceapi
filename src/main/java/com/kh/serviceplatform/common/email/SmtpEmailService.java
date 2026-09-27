package com.kh.serviceplatform.common.email;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender javaMailSender;
    private final String mailHost;
    private final int mailPort;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailFrom;

    public SmtpEmailService(
            @Autowired(required = false) JavaMailSender javaMailSender,
            @Value("${spring.mail.host:smtp.gmail.com}") String mailHost,
            @Value("${spring.mail.port:587}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom
    ) {
        this.javaMailSender = javaMailSender;
        this.mailHost = (mailHost != null && !mailHost.isBlank()) ? mailHost.trim() : "smtp.gmail.com";
        this.mailPort = mailPort > 0 ? mailPort : 587;
        this.mailUsername = mailUsername != null ? mailUsername.trim() : "";
        this.mailPassword = mailPassword != null ? mailPassword.trim() : "";
        this.mailFrom = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : this.mailUsername;
    }

    @PostConstruct
    public void validateAndLogDiagnostics() {
        log.info("Email provider: Gmail SMTP (host={}, port={}, from configured={})",
                mailHost, mailPort, !mailFrom.isBlank());

        if (mailUsername.isBlank() || mailPassword.isBlank()) {
            log.warn("SMTP configuration warning: MAIL_USERNAME or MAIL_PASSWORD is not configured. " +
                    "Outgoing password-reset emails will fail until valid Gmail credentials are provided in environment variables.");
        } else {
            log.info("SMTP credentials: configured");
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String recipient, String resetUrl) {
        sendPasswordResetEmail(recipient, "User", resetUrl, 15);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String recipient, String recipientName, String resetUrl, int expirationMinutes) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send password reset email: recipient address is null or empty");
            return;
        }

        log.info("Dispatching password reset email to: {}", recipient);

        if (javaMailSender == null) {
            log.error("Password reset email delivery failed for recipient {}: JavaMailSender bean is not available", recipient);
            return;
        }

        String displayName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "there";
        String subject = "Reset your password";
        String htmlContent = buildHtmlContent(displayName, resetUrl, expirationMinutes);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setTo(recipient);
            helper.setFrom(mailFrom);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Password reset email sent successfully to: {}", recipient);
        } catch (MailAuthenticationException ex) {
            log.error("Password reset email delivery failed for recipient {}: SMTP authentication failure", recipient);
        } catch (MailException | MessagingException ex) {
            String errorType = determineErrorCategory(ex);
            log.error("Password reset email delivery failed for recipient {}: {}", recipient, errorType);
        } catch (Exception ex) {
            String errorType = determineErrorCategory(ex);
            log.error("Password reset email delivery failed for recipient {}: {}", recipient, errorType);
        }
    }

    private String determineErrorCategory(Throwable throwable) {
        if (hasCause(throwable, SocketTimeoutException.class) || matchesText(throwable, "timed out", "timeout")) {
            return "SMTP connection timeout";
        }
        if (matchesText(throwable, "535", "authentication failed", "not accepted")) {
            return "SMTP authentication failure";
        }
        if (matchesText(throwable, "couldn't connect", "connection refused", "connectexception")) {
            return "SMTP connection failure";
        }
        return "other mail failure";
    }

    private boolean hasCause(Throwable root, Class<? extends Throwable> targetClass) {
        Throwable current = root;
        while (current != null) {
            if (targetClass.isInstance(current)) {
                return true;
            }
            if (current instanceof MailSendException sendEx && sendEx.getMessageExceptions() != null) {
                for (Exception nested : sendEx.getMessageExceptions()) {
                    if (hasCause(nested, targetClass)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean matchesText(Throwable root, String... patterns) {
        Throwable current = root;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                for (String p : patterns) {
                    if (lower.contains(p)) {
                        return true;
                    }
                }
            }
            if (current instanceof MailSendException sendEx && sendEx.getMessageExceptions() != null) {
                for (Exception nested : sendEx.getMessageExceptions()) {
                    if (matchesText(nested, patterns)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
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
