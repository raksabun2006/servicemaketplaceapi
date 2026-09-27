package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.response.MessageResponse;
import com.kh.serviceplatform.features.auth.dto.ForgotPasswordRequest;
import com.kh.serviceplatform.features.auth.dto.ResetPasswordRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    @Test
    void forgotPassword_shouldCallAuthServiceAndReturnOk() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");
        MessageResponse expectedResponse = MessageResponse.of("If an account exists with this email, a password reset link has been sent.");

        when(authService.forgotPassword(request)).thenReturn(expectedResponse);

        ResponseEntity<MessageResponse> response = userController.forgotPassword(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).forgotPassword(request);
    }

    @Test
    void resetPassword_shouldCallAuthServiceAndReturnOk() {
        ResetPasswordRequest request = new ResetPasswordRequest("token-123", "NewPassword123!", "NewPassword123!");
        MessageResponse expectedResponse = MessageResponse.of("Password has been reset successfully.");

        when(authService.resetPassword(request)).thenReturn(expectedResponse);

        ResponseEntity<MessageResponse> response = userController.resetPassword(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).resetPassword(request);
    }
}
