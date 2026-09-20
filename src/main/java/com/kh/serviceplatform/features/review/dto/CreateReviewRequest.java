package com.kh.serviceplatform.features.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a review for a completed booking")
public record CreateReviewRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        @Schema(description = "Star rating from 1 to 5", example = "5")
        Integer rating,

        @Size(max = 2000, message = "Comment must not exceed 2000 characters")
        @Schema(description = "Review comment or feedback", example = "Outstanding cleaning service, arrived on time and very professional!")
        String comment
) {
}
