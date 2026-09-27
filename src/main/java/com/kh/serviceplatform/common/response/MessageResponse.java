package com.kh.serviceplatform.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessageResponse(
        @Schema(description = "Response message", example = "If an account exists with this email, a password reset link has been sent.")
        String message
) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
