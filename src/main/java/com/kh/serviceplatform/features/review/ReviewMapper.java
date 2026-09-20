package com.kh.serviceplatform.features.review;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.review.dto.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }

        Booking booking = review.getBooking();
        User customer = review.getCustomer();
        ProviderProfile provider = review.getProvider();

        return new ReviewResponse(
                review.getId(),
                booking != null ? booking.getId() : null,
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getAvatarUrl() : null,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getBusinessName() : null,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
