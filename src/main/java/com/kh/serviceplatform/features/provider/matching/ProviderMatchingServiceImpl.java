package com.kh.serviceplatform.features.provider.matching;

import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.matching.dto.RecommendedProviderResponse;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderMatchingServiceImpl implements ProviderMatchingService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ProviderProfileRepository providerProfileRepository;

    @Override
    public List<RecommendedProviderResponse> getRecommendedProviders(UUID requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        List<ProviderProfile> availableProviders = providerProfileRepository.findAvailableWithCoordinates();
        if (availableProviders.isEmpty()) {
            availableProviders = providerProfileRepository.findAll().stream()
                    .filter(ProviderProfile::isAvailable)
                    .toList();
        }

        Double reqLat = request.getLatitude();
        Double reqLon = request.getLongitude();

        record ScoredProvider(ProviderProfile provider, double score, Double distanceKm) {}

        List<ScoredProvider> scoredList = new ArrayList<>();

        for (ProviderProfile provider : availableProviders) {
            // Check if provider is available
            if (!provider.isAvailable() && provider.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                continue;
            }

            Double distanceKm = null;
            if (reqLat != null && reqLon != null && provider.getLatitude() != null && provider.getLongitude() != null) {
                distanceKm = GeoUtils.calculateDistanceKm(reqLat, reqLon, provider.getLatitude(), provider.getLongitude());
                double maxRadius = provider.getServiceRadiusKm() != null ? provider.getServiceRadiusKm() : 15.0;
                // Allow generous discovery radius (within maxRadius or 25km)
                if (distanceKm > Math.max(maxRadius, 25.0)) {
                    continue;
                }
            } else if (request.getCity() != null && provider.getCity() != null) {
                if (!request.getCity().equalsIgnoreCase(provider.getCity())) {
                    continue;
                }
            }

            double score = computeMatchScore(request, provider, distanceKm);
            scoredList.add(new ScoredProvider(provider, score, distanceKm));
        }

        scoredList.sort(Comparator.comparingDouble(ScoredProvider::score).reversed());

        return scoredList.stream()
                .map(sp -> {
                    ProviderProfile p = sp.provider();
                    return new RecommendedProviderResponse(
                            p.getId(),
                            p.getBusinessName(),
                            p.getUser() != null ? p.getUser().getFullName() : null,
                            p.getUser() != null ? p.getUser().getAvatarUrl() : null,
                            p.getAverageRating() != null ? p.getAverageRating() : 0.0,
                            p.getCompletedServices() != null ? p.getCompletedServices() : 0,
                            p.getExperienceYears() != null ? p.getExperienceYears() : 0,
                            sp.distanceKm(),
                            p.getAvailabilityStatus() != null ? p.getAvailabilityStatus() : AvailabilityStatus.AVAILABLE,
                            p.isVerified()
                    );
                })
                .toList();
    }

    @Override
    public List<ProviderProfile> findMatchingProvidersForRequest(ServiceRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }

        List<ProviderProfile> availableProviders = providerProfileRepository.findAvailableWithCoordinates();
        if (availableProviders.isEmpty()) {
            return providerProfileRepository.findAll().stream()
                    .filter(ProviderProfile::isAvailable)
                    .toList();
        }

        Double reqLat = request.getLatitude();
        Double reqLon = request.getLongitude();

        List<ProviderProfile> matched = new ArrayList<>();

        for (ProviderProfile provider : availableProviders) {
            if (!provider.isAvailable()) {
                continue;
            }

            if (reqLat != null && reqLon != null && provider.getLatitude() != null && provider.getLongitude() != null) {
                double distanceKm = GeoUtils.calculateDistanceKm(reqLat, reqLon, provider.getLatitude(), provider.getLongitude());
                double maxRadius = provider.getServiceRadiusKm() != null ? provider.getServiceRadiusKm() : 15.0;
                if (distanceKm <= maxRadius) {
                    matched.add(provider);
                }
            } else if (request.getCity() != null && provider.getCity() != null) {
                if (request.getCity().equalsIgnoreCase(provider.getCity())) {
                    matched.add(provider);
                }
            } else {
                matched.add(provider);
            }
        }

        return matched;
    }

    private double computeMatchScore(ServiceRequest request, ProviderProfile provider, Double distanceKm) {
        double score = 50.0;

        // Distance factor (closer is higher)
        if (distanceKm != null) {
            score += Math.max(0, 30.0 - (distanceKm * 1.5));
        }

        // Rating factor (up to 25 points)
        if (provider.getAverageRating() != null && provider.getAverageRating() > 0) {
            score += provider.getAverageRating() * 5.0;
        }

        // Verification bonus (15 points)
        if (provider.isVerified()) {
            score += 15.0;
        }

        // Experience bonus (up to 10 points)
        if (provider.getExperienceYears() != null && provider.getExperienceYears() > 0) {
            score += Math.min(provider.getExperienceYears() * 1.5, 10.0);
        }

        // Completed services bonus (up to 10 points)
        if (provider.getCompletedServices() != null && provider.getCompletedServices() > 0) {
            score += Math.min(provider.getCompletedServices() * 0.5, 10.0);
        }

        return score;
    }
}
