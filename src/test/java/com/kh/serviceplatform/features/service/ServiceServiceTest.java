package com.kh.serviceplatform.features.service;

import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.service.dto.CreateServiceRequest;
import com.kh.serviceplatform.features.service.dto.ServiceResponse;
import com.kh.serviceplatform.features.service.dto.UpdateServiceRequest;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ServiceMapper mapper;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private User providerUser;
    private ProviderProfile providerProfile;
    private ServiceOffer serviceOffer;

    @BeforeEach
    void setUp() {
        providerUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Meas Sopheak")
                .email("sopheak@example.com")
                .build();

        providerProfile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(providerUser)
                .businessName("Sopheak Electricals")
                .isAvailable(true)
                .build();

        serviceOffer = ServiceOffer.builder()
                .id(UUID.randomUUID())
                .provider(providerProfile)
                .name("AC Repair")
                .description("Full maintenance and gas refill")
                .category(ServiceCategory.AC_REPAIR)
                .price(new BigDecimal("35.00"))
                .durationMinutes(60)
                .isAvailable(true)
                .build();
    }

    @Test
    void shouldCreateServiceSuccessfully() {
        CreateServiceRequest request = new CreateServiceRequest(
                "AC Repair",
                "Full maintenance",
                ServiceCategory.AC_REPAIR,
                new BigDecimal("35.00"),
                60,
                null,
                true
        );

        when(providerProfileRepository.findByUserId(providerUser.getId())).thenReturn(Optional.of(providerProfile));
        when(serviceRepository.save(any(ServiceOffer.class))).thenReturn(serviceOffer);
        when(mapper.toResponse(serviceOffer)).thenReturn(new ServiceResponse(
                serviceOffer.getId(),
                providerProfile.getId(),
                "Sopheak Electricals",
                "Meas Sopheak",
                "AC Repair",
                "Full maintenance",
                ServiceCategory.AC_REPAIR,
                new BigDecimal("35.00"),
                60,
                null,
                null,
                true,
                null,
                null
        ));

        ServiceResponse response = serviceService.createService(providerUser.getId(), request);

        assertNotNull(response);
        assertEquals("AC Repair", response.name());
        verify(serviceRepository).save(any(ServiceOffer.class));
    }

    @Test
    void shouldPreventOtherProviderFromUpdatingService() {
        UUID otherUserId = UUID.randomUUID();
        UpdateServiceRequest request = new UpdateServiceRequest(
                "Hacked Service", null, null, null, null, null, null
        );

        when(serviceRepository.findById(serviceOffer.getId())).thenReturn(Optional.of(serviceOffer));

        assertThrows(ForbiddenException.class, () ->
                serviceService.updateMyService(otherUserId, serviceOffer.getId(), request));
    }

    @Test
    void shouldPreventOtherProviderFromDeletingService() {
        UUID otherUserId = UUID.randomUUID();

        when(serviceRepository.findById(serviceOffer.getId())).thenReturn(Optional.of(serviceOffer));

        assertThrows(ForbiddenException.class, () ->
                serviceService.deleteMyService(otherUserId, serviceOffer.getId()));
    }

    @Test
    void shouldDeleteMyServiceSuccessfully() {
        when(serviceRepository.findById(serviceOffer.getId())).thenReturn(Optional.of(serviceOffer));

        serviceService.deleteMyService(providerUser.getId(), serviceOffer.getId());

        verify(serviceRepository).delete(serviceOffer);
    }

    @Test
    void shouldGetPublicServices() {
        Page<ServiceOffer> page = new PageImpl<>(List.of(serviceOffer));
        when(serviceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toResponse(any(ServiceOffer.class))).thenReturn(mock(ServiceResponse.class));

        Page<ServiceResponse> result = serviceService.getPublicServices(
                "repair", ServiceCategory.AC_REPAIR, null, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldThrowWhenPublicServiceIsUnavailable() {
        serviceOffer.setAvailable(false);
        when(serviceRepository.findById(serviceOffer.getId())).thenReturn(Optional.of(serviceOffer));

        assertThrows(ResourceNotFoundException.class, () ->
                serviceService.getPublicServiceById(serviceOffer.getId()));
    }
}
