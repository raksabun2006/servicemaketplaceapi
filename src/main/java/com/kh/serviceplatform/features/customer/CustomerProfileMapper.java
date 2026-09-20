package com.kh.serviceplatform.features.customer;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.customer.dto.CustomerProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerProfileMapper {

    public CustomerProfileResponse toResponse(CustomerProfile profile) {
        if (profile == null) {
            return null;
        }

        User user = profile.getUser();

        return new CustomerProfileResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getAvatarUrl() : null,
                profile.getPreferredLanguage(),
                profile.getPreferredCurrency(),
                profile.getAddress(),
                profile.getCity(),
                profile.getPostalCode(),
                profile.getNotes(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
