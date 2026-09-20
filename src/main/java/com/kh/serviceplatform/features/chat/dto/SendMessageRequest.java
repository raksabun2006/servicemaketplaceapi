package com.kh.serviceplatform.features.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to send a chat message")
public record SendMessageRequest(
        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        @Schema(description = "Message body text", example = "Hi, I will arrive at 2 PM as requested.")
        String message
) {
}
