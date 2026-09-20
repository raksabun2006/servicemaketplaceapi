package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationResponse;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationReviewRequest;
import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin Provider Applications", description = "Admin endpoints for reviewing and managing provider applications")
@RestController
@RequestMapping("/api/v1/admin/provider-applications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProviderApplicationController {

    private final ProviderApplicationService applicationService;

    @GetMapping
    @Operation(summary = "List all provider applications with optional filtering")
    public Page<ProviderApplicationResponse> getApplications(
            @RequestParam(required = false) ProviderApplicationStatus status,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return applicationService.getApplications(status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a provider application")
    public ProviderApplicationResponse getApplicationById(@PathVariable UUID id) {
        return applicationService.getApplicationById(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending provider application and upgrade user to PROVIDER")
    public ProviderApplicationResponse approveApplication(@PathVariable UUID id) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        return applicationService.approveApplication(id, adminId);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending provider application with a reason")
    public ProviderApplicationResponse rejectApplication(
            @PathVariable UUID id,
            @Valid @RequestBody ProviderApplicationReviewRequest request
    ) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        return applicationService.rejectApplication(id, adminId, request);
    }
}
