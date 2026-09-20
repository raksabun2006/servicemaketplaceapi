package com.kh.serviceplatform.features.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for admin provider review action")
public record ProviderReviewActionRequest(
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @Schema(description = "Reason or remarks for approval/rejection", example = "Documents verified successfully.")
        String reason
) {
}
