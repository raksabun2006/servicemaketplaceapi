package com.kh.serviceplatform.common.email;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
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
        this.mailHost = mailHost != null ? mailHost.trim() : "smtp.gmail.com";
        this.mailPort = mailPort;
        this.mailUsername = mailUsername != null ? mailUsername.trim() : "";
        this.mailPassword = mailPassword != null ? mailPassword.trim() : "";
        this.mailFrom = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : this.mailUsername;
    }

    @PostConstruct
    public void validateAndLogDiagnostics() {
        log.info("Email provider: SMTP");
        log.info("SMTP host: {}", mailHost);
        log.info("SMTP port: {}", mailPort);
        log.info("MAIL_FROM: {}", (!mailFrom.isBlank()) ? "configured" : "NOT CONFIGURED");

        if (mailHost.isBlank()) {
            throw new IllegalStateException("SMTP host is missing. Configure MAIL_HOST in environment variables.");
        }
        if (mailUsername.isBlank()) {
            throw new IllegalStateException("SMTP username is missing. Configure MAIL_USERNAME in environment variables.");
        }
        if (mailPassword.isBlank()) {
            throw new IllegalStateException("SMTP password is missing. Configure MAIL_PASSWORD in environment variables.");
        }
        if (mailFrom.isBlank()) {
            throw new IllegalStateException("Sender email is missing. Configure MAIL_FROM or MAIL_USERNAME in environment variables.");
        }
    }

    @Override
    public void sendPasswordResetEmail(String recipient, String resetUrl) {
        sendPasswordResetEmail(recipient, "User", resetUrl, 15);
    }

    @Override
    public void sendPasswordResetEmail(String recipient, String recipientName, String resetUrl, int expirationMinutes) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send password reset email: recipient email is missing");
            return;
        }

        // Security requirement: NEVER log reset tokens or complete reset URLs
        log.info("Dispatching password reset email to: {}", recipient);

        if (javaMailSender == null) {
            log.error("JavaMailSender is not configured. Unable to send email via SMTP.");
            throw new IllegalStateException("JavaMailSender bean is not available");
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
            log.info("Password reset email successfully sent via SMTP to {}", recipient);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failure while sending password reset email to {}: check MAIL_USERNAME and App Password", recipient);
            log.error("Failed to send password reset email", ex);
            throw ex;
        } catch (MailSendException ex) {
            String category = classifyMailSendException(ex);
            log.error("SMTP {} while sending password reset email to {}: {}", category, recipient, ex.getMessage());
            log.error("Failed to send password reset email", ex);
            throw ex;
        } catch (MailException ex) {
            log.error("SMTP failure while sending password reset email to {}: {}", recipient, ex.getMessage());
            log.error("Failed to send password reset email", ex);
            throw ex;
        } catch (MessagingException ex) {
            log.error("MIME message preparation failure for recipient {}: {}", recipient, ex.getMessage());
            log.error("Failed to send password reset email", ex);
            throw new RuntimeException("Failed to prepare password reset email", ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending password reset email to {}: {}", recipient, ex.getMessage());
            log.error("Failed to send password reset email", ex);
            throw new RuntimeException("Failed to send password reset email", ex);
        }
    }

    private String classifyMailSendException(MailSendException ex) {
        if (ex.getMessageExceptions() != null) {
            for (Exception subEx : ex.getMessageExceptions()) {
                String subName = subEx.getClass().getSimpleName();
                String subMsg = subEx.getMessage() != null ? subEx.getMessage().toLowerCase() : "";
                if (subName.contains("Connect") || subMsg.contains("couldn't connect") || subMsg.contains("connection refused")) {
                    return "connection failure";
                }
                if (subName.contains("Timeout") || subMsg.contains("timed out") || subMsg.contains("timeout")) {
                    return "timeout";
                }
                if (subName.contains("Authentication") || subMsg.contains("authenticate") || subMsg.contains("password")) {
                    return "authentication failure";
                }
                if (subName.contains("SendFailed") || subMsg.contains("invalid addresses") || subMsg.contains("recipient")) {
                    return "recipient failure";
                }
            }
        }
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("connect")) {
            return "connection failure";
        }
        if (msg.contains("timeout")) {
            return "timeout";
        }
        return "delivery failure";
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
