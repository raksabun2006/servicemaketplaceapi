package com.kh.serviceplatform.features.review;

import com.kh.serviceplatform.features.review.dto.CreateReviewRequest;
import com.kh.serviceplatform.features.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID customerUserId, UUID bookingId, CreateReviewRequest request);

    Page<ReviewResponse> getReviewsByProviderId(UUID providerId, Pageable pageable);
}
