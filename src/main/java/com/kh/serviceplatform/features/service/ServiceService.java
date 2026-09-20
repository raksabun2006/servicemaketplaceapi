package com.kh.serviceplatform.features.service;

import com.kh.serviceplatform.features.service.dto.CreateServiceRequest;
import com.kh.serviceplatform.features.service.dto.ServiceResponse;
import com.kh.serviceplatform.features.service.dto.UpdateServiceRequest;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface ServiceService {

    // Provider operations
    ServiceResponse createService(UUID userId, CreateServiceRequest request);

    Page<ServiceResponse> getMyServices(UUID userId, Pageable pageable);

    ServiceResponse getMyServiceById(UUID userId, UUID serviceId);

    ServiceResponse updateMyService(UUID userId, UUID serviceId, UpdateServiceRequest request);

    void deleteMyService(UUID userId, UUID serviceId);

    // Public browsing operations
    Page<ServiceResponse> getPublicServices(
            String search,
            ServiceCategory category,
            UUID providerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    ServiceResponse getPublicServiceById(UUID serviceId);

    Page<ServiceResponse> getPublicServicesByProviderId(UUID providerId, Pageable pageable);
}
