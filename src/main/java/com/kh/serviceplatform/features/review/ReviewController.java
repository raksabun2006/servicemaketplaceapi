package com.kh.serviceplatform.features.review;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.review.dto.CreateReviewRequest;
import com.kh.serviceplatform.features.review.dto.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reviews", description = "Endpoints for reviewing completed bookings and viewing provider reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit a review for a completed booking", description = "Customer submits a rating and review for a completed booking",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Booking not completed, duplicate review, or invalid rating"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not your booking or not customer"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ReviewResponse createReview(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return reviewService.createReview(currentUserId, bookingId, request);
    }

    @GetMapping("/api/v1/providers/{providerId}/reviews")
    @Operation(summary = "Get reviews for a provider", description = "Publicly browse paginated reviews and ratings for a specific provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reviews retrieved"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public Page<ReviewResponse> getReviewsByProviderId(
            @PathVariable UUID providerId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reviewService.getReviewsByProviderId(providerId, pageable);
    }
}
