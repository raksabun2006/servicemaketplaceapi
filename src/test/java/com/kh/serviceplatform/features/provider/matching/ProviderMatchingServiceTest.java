package com.kh.serviceplatform.features.provider.matching;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import com.kh.serviceplatform.features.provider.matching.dto.RecommendedProviderResponse;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderMatchingServiceTest {

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private ProviderMatchingServiceImpl providerMatchingService;

    private ServiceRequest serviceRequest;
    private ProviderProfile provider1;
    private ProviderProfile provider2;

    @BeforeEach
    void setUp() {
        serviceRequest = ServiceRequest.builder()
                .id(UUID.randomUUID())
                .title("Need AC fixed ASAP")
                .category(ServiceCategory.AC_REPAIR)
                .latitude(11.5564)
                .longitude(104.9282)
                .city("Phnom Penh")
                .preferredDate(LocalDate.now())
                .build();

        provider1 = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).fullName("Pro 1").build())
                .businessName("AC Masters")
                .latitude(11.5580)
                .longitude(104.9290)
                .serviceRadiusKm(15.0)
                .isAvailable(true)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isVerified(true)
                .verificationStatus(ProviderVerificationStatus.VERIFIED)
                .averageRating(4.9)
                .totalReviews(25)
                .experienceYears(6)
                .completedServices(40)
                .build();

        provider2 = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).fullName("Pro 2").build())
                .businessName("General Handyman")
                .latitude(11.5700)
                .longitude(104.9350)
                .serviceRadiusKm(5.0)
                .isAvailable(true)
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .isVerified(false)
                .averageRating(3.5)
                .totalReviews(2)
                .experienceYears(1)
                .completedServices(2)
                .build();
    }

    @Test
    void shouldRankHigherScoringProviderFirst() {
        when(serviceRequestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(providerProfileRepository.findAvailableWithCoordinates()).thenReturn(List.of(provider1, provider2));

        List<RecommendedProviderResponse> recommended = providerMatchingService.getRecommendedProviders(serviceRequest.getId());

        assertNotNull(recommended);
        assertFalse(recommended.isEmpty());
        // Provider 1 should be ranked higher due to verification, higher rating, experience, and proximity
        assertEquals(provider1.getId(), recommended.get(0).providerId());
        assertTrue(recommended.get(0).rating() >= 4.5);
    }
}
