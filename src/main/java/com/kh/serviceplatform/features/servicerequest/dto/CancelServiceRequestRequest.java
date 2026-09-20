package com.kh.serviceplatform.features.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for cancelling a service request")
public record CancelServiceRequestRequest(
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @Schema(description = "Reason for cancellation", example = "Fixed the issue myself.")
        String reason
) {
}
