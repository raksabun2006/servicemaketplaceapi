package com.kh.serviceplatform.features.customer.dto;

import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
        @Size(max = 150)
        String fullName,

        @Size(max = 20)
        String phone,

        @Size(max = 500)
        String avatarUrl,

        @Size(max = 10)
        String preferredLanguage,

        @Size(max = 10)
        String preferredCurrency,

        @Size(max = 255)
        String address,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String district,

        @Size(max = 20)
        String postalCode,

        @Size(max = 2000)
        String notes
) {
}
