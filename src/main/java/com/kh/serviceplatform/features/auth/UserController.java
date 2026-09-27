package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.response.MessageResponse;
import com.kh.serviceplatform.features.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user as CUSTOMER or submit a PROVIDER onboarding registration for review")
    public AuthResponse createNew(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT access/refresh tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping({"/google", "/google-login"})
    @Operation(summary = "Authenticate user via Google ID token and receive JWT access/refresh tokens")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request a password reset link via email",
            description = "Initiates the password reset process. If an account is associated with the email, a secure one-time link is sent. Returns a generic message to prevent account enumeration.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Generic success response",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"If an account exists with this email, a password reset link has been sent.\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using one-time token",
            description = "Resets the user's password using the token received in the reset email. The token can only be used once and expires shortly after issuance.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"Password has been reset successfully.\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token, or passwords do not match")
    })
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
