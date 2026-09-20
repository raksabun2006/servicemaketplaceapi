package com.kh.serviceplatform.features.service;

import com.kh.serviceplatform.features.service.dto.ServiceResponse;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = "Public Services", description = "Public endpoints for browsing available services and providers' catalog")
@RestController
@RequiredArgsConstructor
public class PublicServiceController {

    private final ServiceService serviceService;

    @GetMapping("/api/v1/services")
    @Operation(summary = "Browse public services", description = "Search, filter, and page through active and available services")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of services retrieved")
    })
    public Page<ServiceResponse> getPublicServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return serviceService.getPublicServices(search, category, providerId, minPrice, maxPrice, pageable);
    }

    @GetMapping("/api/v1/services/{serviceId}")
    @Operation(summary = "Get service details by ID", description = "Retrieve public details of an active service")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service details found"),
            @ApiResponse(responseCode = "404", description = "Service not found or inactive")
    })
    public ServiceResponse getPublicServiceById(@PathVariable UUID serviceId) {
        return serviceService.getPublicServiceById(serviceId);
    }

    @GetMapping("/api/v1/providers/{providerId}/services")
    @Operation(summary = "Get services by provider ID", description = "Browse all active services offered by a specific provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of provider's services retrieved")
    })
    public Page<ServiceResponse> getServicesByProviderId(
            @PathVariable UUID providerId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return serviceService.getPublicServicesByProviderId(providerId, pageable);
    }
}
