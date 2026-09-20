package com.kh.serviceplatform.features.provider.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderApplicationReviewRequest(
        @NotBlank(message = "Rejection reason cannot be blank")
        @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
        String reason
) {
}
