package com.kh.serviceplatform.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleMailException_shouldReturnGeneric500ResponseWithoutLeakingSmtpDetails() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/forgot-password");

        MailSendException mailEx = new MailSendException("Couldn't connect to host, port: smtp.gmail.com, 587");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMailException(mailEx, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        assertEquals("/api/v1/auth/forgot-password", body.path());

        // Ensure safe generic message
        assertFalse(body.message().contains("smtp.gmail.com"));
        assertFalse(body.message().contains("Couldn't connect"));
        assertFalse(body.message().contains("587"));
        assertEquals("An error occurred while sending the email. Please try again later.", body.message());
    }
}
