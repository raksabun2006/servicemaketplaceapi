package com.kh.serviceplatform.features.provider.matching;

import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.matching.dto.RecommendedProviderResponse;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;

import java.util.List;
import java.util.UUID;

public interface ProviderMatchingService {

    List<RecommendedProviderResponse> getRecommendedProviders(UUID requestId);

    List<ProviderProfile> findMatchingProvidersForRequest(ServiceRequest request);
}
