package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationRequest;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationResponse;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationReviewRequest;
import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProviderApplicationService {

    ProviderApplicationResponse submitApplication(UUID userId, ProviderApplicationRequest request);

    ProviderApplicationResponse getMyLatestApplication(UUID userId);

    ProviderApplicationResponse updateMyPendingApplication(UUID userId, ProviderApplicationRequest request);

    ProviderApplicationResponse cancelMyApplication(UUID userId);

    Page<ProviderApplicationResponse> getApplications(ProviderApplicationStatus status, String search, Pageable pageable);

    ProviderApplicationResponse getApplicationById(UUID id);

    ProviderApplicationResponse approveApplication(UUID id, UUID adminId);

    ProviderApplicationResponse rejectApplication(UUID id, UUID adminId, ProviderApplicationReviewRequest request);
}
