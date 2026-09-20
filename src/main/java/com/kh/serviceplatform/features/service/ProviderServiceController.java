package com.kh.serviceplatform.features.service;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.service.dto.CreateServiceRequest;
import com.kh.serviceplatform.features.service.dto.ServiceResponse;
import com.kh.serviceplatform.features.service.dto.UpdateServiceRequest;
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

@Tag(name = "Provider Services", description = "Endpoints for providers to manage their service catalog")
@RestController
@RequestMapping("/api/v1/providers/me/services")
@PreAuthorize("hasRole('PROVIDER')")
@RequiredArgsConstructor
public class ProviderServiceController {

    private final ServiceService serviceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a service offering", description = "Create a new service offering for the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role")
    })
    public ServiceResponse createService(@Valid @RequestBody CreateServiceRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return serviceService.createService(currentUserId, request);
    }

    @GetMapping
    @Operation(summary = "List my services", description = "List all services created by the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of services retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role")
    })
    public Page<ServiceResponse> getMyServices(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return serviceService.getMyServices(currentUserId, pageable);
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Get my service by ID", description = "Retrieve a specific service owned by the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service details retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner of this service"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ServiceResponse getMyServiceById(@PathVariable UUID serviceId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return serviceService.getMyServiceById(currentUserId, serviceId);
    }

    @PutMapping("/{serviceId}")
    @Operation(summary = "Update my service", description = "Update a service owned by the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner of this service"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ServiceResponse updateMyService(
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return serviceService.updateMyService(currentUserId, serviceId, request);
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my service", description = "Delete a service owned by the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Service deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner of this service"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public void deleteMyService(@PathVariable UUID serviceId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        serviceService.deleteMyService(currentUserId, serviceId);
    }
}
