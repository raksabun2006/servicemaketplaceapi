package com.kh.serviceplatform.common.email;

import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpEmailService smtpEmailService;

    private static final String MAIL_HOST = "smtp.gmail.com";
    private static final int MAIL_PORT = 587;
    private static final String MAIL_USERNAME = "khmerservice@gmail.com";
    private static final String MAIL_PASSWORD = "secret-google-app-password";
    private static final String MAIL_FROM = "khmerservice@gmail.com";

    @BeforeEach
    void setUp() {
        smtpEmailService = new SmtpEmailService(
                javaMailSender,
                true,
                MAIL_HOST,
                MAIL_PORT,
                MAIL_USERNAME,
                MAIL_PASSWORD,
                MAIL_FROM
        );
    }

    @Test
    void sendPasswordResetEmail_shouldSetCorrectRecipientSenderSubjectAndHtmlWithResetUrl() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        String recipient = "customer@example.com";
        String resetUrl = "https://servicemaketplacefront.vercel.app/reset-password?token=secureToken123456";
        int expirationMinutes = 15;

        smtpEmailService.sendPasswordResetEmail(recipient, "John Doe", resetUrl, expirationMinutes);

        verify(javaMailSender).send(mimeMessage);

        // Verify recipient
        Address[] recipients = mimeMessage.getAllRecipients();
        assertNotNull(recipients);
        assertEquals(1, recipients.length);
        assertEquals(recipient, recipients[0].toString());

        // Verify sender
        Address[] fromAddresses = mimeMessage.getFrom();
        assertNotNull(fromAddresses);
        assertEquals(1, fromAddresses.length);
        assertEquals(MAIL_FROM, fromAddresses[0].toString());

        // Verify subject
        assertEquals("Reset your password", mimeMessage.getSubject());

        // Verify email content contains required sections and reset URL
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(outputStream);
        String rawContent = outputStream.toString(StandardCharsets.UTF_8);

        assertTrue(rawContent.contains("Khmer Service"));
        assertTrue(rawContent.contains("Password Reset"));
        assertTrue(rawContent.contains("We received a request to reset your password."));
        assertTrue(rawContent.contains("Click the button below to create a new password."));
        assertTrue(rawContent.contains("Reset Password"));
        assertTrue(rawContent.contains(resetUrl));
        assertTrue(rawContent.contains("This link will expire after 15 minutes."));
        assertTrue(rawContent.contains("If you did not request a password reset, you can safely ignore this email."));

        // Verify no emojis are in the raw content
        assertFalse(rawContent.matches(".*[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+.*"));
    }

    @Test
    void sendPasswordResetEmail_whenEmailDisabled_shouldSkipSending() {
        SmtpEmailService disabledService = new SmtpEmailService(
                javaMailSender,
                false,
                MAIL_HOST,
                MAIL_PORT,
                MAIL_USERNAME,
                MAIL_PASSWORD,
                MAIL_FROM
        );

        disabledService.sendPasswordResetEmail("customer@example.com", "https://frontend.com/reset");

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_overloadedMethod_shouldDelegateWithDefaultValues() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        String recipient = "customer@example.com";
        String resetUrl = "https://servicemaketplacefront.vercel.app/reset-password?token=secureToken789";

        smtpEmailService.sendPasswordResetEmail(recipient, resetUrl);

        verify(javaMailSender).send(mimeMessage);
        assertEquals("Reset your password", mimeMessage.getSubject());
    }

    @Test
    void sendPasswordResetEmail_whenRecipientIsMissing_shouldNotSendMessage() {
        smtpEmailService.sendPasswordResetEmail(null, "https://frontend.com/reset");
        smtpEmailService.sendPasswordResetEmail("   ", "https://frontend.com/reset");

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_whenSmtpAuthenticationFails_shouldLogAndNotThrow() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("535 5.7.8 Username and Password not accepted"))
                .when(javaMailSender).send(any(MimeMessage.class));

        // Must not throw out of async method to prevent SimpleAsyncUncaughtExceptionHandler
        assertDoesNotThrow(() ->
                smtpEmailService.sendPasswordResetEmail("customer@example.com", "https://example.com/reset")
        );
    }

    @Test
    void sendPasswordResetEmail_whenSmtpConnectionFails_shouldLogAndNotThrow() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Couldn't connect to host, port: smtp.gmail.com, 587"))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                smtpEmailService.sendPasswordResetEmail("customer@example.com", "https://example.com/reset")
        );
    }

    @Test
    void sendPasswordResetEmail_whenSmtpTimeoutOccurs_shouldLogAndNotThrow() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        MailSendException timeoutEx = new MailSendException("Mail send failed", new SocketTimeoutException("Connect timed out"));
        doThrow(timeoutEx).when(javaMailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                smtpEmailService.sendPasswordResetEmail("customer@example.com", "https://example.com/reset")
        );
    }

    @Test
    void sendPasswordResetEmail_whenJavaMailSenderIsNull_shouldLogAndNotThrow() {
        SmtpEmailService serviceWithoutSender = new SmtpEmailService(
                null,
                true,
                MAIL_HOST,
                MAIL_PORT,
                MAIL_USERNAME,
                MAIL_PASSWORD,
                MAIL_FROM
        );

        assertDoesNotThrow(() ->
                serviceWithoutSender.sendPasswordResetEmail("customer@example.com", "https://example.com/reset")
        );
    }

    @Test
    void validateAndLogDiagnostics_withValidConfiguration_shouldNotThrow() {
        assertDoesNotThrow(() -> smtpEmailService.validateAndLogDiagnostics());
    }

    @Test
    void validateAndLogDiagnostics_withMissingCredentials_shouldLogWarningAndNotThrow() {
        SmtpEmailService serviceMissingCreds = new SmtpEmailService(
                javaMailSender,
                true,
                MAIL_HOST,
                587,
                "",
                "",
                ""
        );

        // Application must stay healthy and not fail startup
        assertDoesNotThrow(serviceMissingCreds::validateAndLogDiagnostics);
    }
}
