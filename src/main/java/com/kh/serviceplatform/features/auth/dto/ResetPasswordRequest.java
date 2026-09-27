package com.kh.serviceplatform.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Schema(description = "Cryptographically secure password reset token from email link", example = "4e_Xf1K-9a0_Bc7Z2dEf-ghIjKlMnOpQrStUvWxYz")
        @NotBlank(message = "Reset token is required")
        String token,

        @Schema(description = "New password", example = "NewStrongPassword123!")
        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String newPassword,

        @Schema(description = "Confirmation of new password", example = "NewStrongPassword123!")
        @NotBlank(message = "Password confirmation is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String confirmPassword
) {
    public String getToken() {
        return token();
    }

    public String getNewPassword() {
        return newPassword();
    }

    public String getConfirmPassword() {
        return confirmPassword();
    }
}
