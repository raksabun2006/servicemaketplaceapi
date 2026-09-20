package com.kh.serviceplatform.features.provider.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProviderProfileRequest(
        @NotBlank
        @Size(max = 150)
        String businessName,

        @Size(max = 3000)
        String bio,

        @Min(0)
        Integer experienceYears,

        @Size(max = 150)
        String serviceArea,

        @DecimalMin(value = "0.00")
        BigDecimal hourlyRate
) {
}
