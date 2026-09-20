package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.matching.ProviderMatchingService;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.dto.*;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository requestRepository;

    @Mock
    private ServiceRequestOfferRepository offerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProviderMatchingService providerMatchingService;

    @Mock
    private ServiceRequestMapper mapper;

    @InjectMocks
    private ServiceRequestServiceImpl serviceRequestService;

    private User customer;
    private User providerUser;
    private ProviderProfile providerProfile;
    private ServiceRequest serviceRequest;
    private ServiceRequestOffer offer;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(UUID.randomUUID())
                .fullName("Customer Sok")
                .email("sok@example.com")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        providerUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Provider Meas")
                .email("meas@example.com")
                .role(UserRole.PROVIDER)
                .status(UserStatus.ACTIVE)
                .build();

        providerProfile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(providerUser)
                .businessName("Meas AC & Appliances")
                .isAvailable(true)
                .build();

        serviceRequest = ServiceRequest.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .title("Air conditioner is not cooling")
                .description("My home air conditioner turns on but does not produce cold air.")
                .category(ServiceCategory.AC_REPAIR)
                .budgetMin(new BigDecimal("20.00"))
                .budgetMax(new BigDecimal("50.00"))
                .preferredDate(LocalDate.of(2026, 9, 20))
                .preferredTime("14:00")
                .address("Street 271, Sangkat Boeung Tumpun")
                .city("Phnom Penh")
                .district("Meanchey")
                .latitude(11.5435)
                .longitude(104.8997)
                .urgent(false)
                .status(ServiceRequestStatus.OPEN)
                .images(new LinkedHashSet<>())
                .offers(new LinkedHashSet<>())
                .build();

        offer = ServiceRequestOffer.builder()
                .id(UUID.randomUUID())
                .serviceRequest(serviceRequest)
                .provider(providerProfile)
                .proposedPrice(new BigDecimal("35.00"))
                .message("I can inspect and repair your air conditioner.")
                .estimatedCompletionTime("2 hours")
                .status(ServiceOfferStatus.PENDING)
                .build();
    }

    @Test
    void shouldCreateServiceRequestSuccessfully() {
        ServiceRequestCreateRequest request = new ServiceRequestCreateRequest(
                "Air conditioner is not cooling",
                "Detailed description",
                ServiceCategory.AC_REPAIR,
                new BigDecimal("20.00"),
                new BigDecimal("50.00"),
                LocalDate.of(2026, 9, 20),
                "14:00",
                "Street 271",
                "Phnom Penh",
                "Meanchey",
                11.5435,
                104.8997,
                false,
                null
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(mapper.toResponse(any(ServiceRequest.class), eq(customer.getId()), eq(UserRole.CUSTOMER), isNull()))
                .thenReturn(mock(ServiceRequestResponse.class));

        ServiceRequestResponse response = serviceRequestService.createRequest(customer.getId(), request);

        assertNotNull(response);
        verify(requestRepository).save(argThat(sr ->
                sr.getStatus() == ServiceRequestStatus.OPEN &&
                sr.getCustomer().equals(customer) &&
                sr.getCategory() == ServiceCategory.AC_REPAIR
        ));
    }

    @Test
    void shouldRejectInvalidCoordinates() {
        ServiceRequestCreateRequest request = new ServiceRequestCreateRequest(
                "Title", "Desc", ServiceCategory.AC_REPAIR,
                null, null, null, null, "Addr", null, null,
                95.0, // Invalid latitude > 90
                104.0, false, null
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () ->
                serviceRequestService.createRequest(customer.getId(), request));
    }

    @Test
    void shouldUpdateOwnServiceRequest() {
        ServiceRequestUpdateRequest update = new ServiceRequestUpdateRequest(
                "Updated Title", "Updated Desc", ServiceCategory.AC_REPAIR,
                null, null, null, null, null, null, null, null, null, false, null
        );

        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(mapper.toResponse(any(ServiceRequest.class), eq(customer.getId()), eq(UserRole.CUSTOMER), isNull()))
                .thenReturn(mock(ServiceRequestResponse.class));

        ServiceRequestResponse response = serviceRequestService.updateRequest(customer.getId(), serviceRequest.getId(), update);

        assertNotNull(response);
        assertEquals("Updated Title", serviceRequest.getTitle());
    }

    @Test
    void shouldPreventOtherCustomerFromUpdatingRequest() {
        UUID otherCustomerId = UUID.randomUUID();
        ServiceRequestUpdateRequest update = new ServiceRequestUpdateRequest(
                "Hacked Title", null, null, null, null, null, null, null, null, null, null, null, false, null
        );

        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));

        assertThrows(ForbiddenException.class, () ->
                serviceRequestService.updateRequest(otherCustomerId, serviceRequest.getId(), update));
    }

    @Test
    void shouldCancelServiceRequestAndWithdrawOffers() {
        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(offerRepository.findByServiceRequestId(serviceRequest.getId())).thenReturn(List.of(offer));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(mapper.toResponse(any(ServiceRequest.class), eq(customer.getId()), eq(UserRole.CUSTOMER), isNull()))
                .thenReturn(mock(ServiceRequestResponse.class));

        serviceRequestService.cancelRequest(customer.getId(), serviceRequest.getId(), new CancelServiceRequestRequest("Solved"));

        assertEquals(ServiceRequestStatus.CANCELLED, serviceRequest.getStatus());
        assertEquals(ServiceOfferStatus.WITHDRAWN, offer.getStatus());
    }

    @Test
    void shouldSearchNearbyRequestsAccuratelyWithinRadius() {
        // Target: 11.5500, 104.9000 (about 0.7 km away from 11.5435, 104.8997)
        when(requestRepository.findByStatusAndHasCoordinates(ServiceRequestStatus.OPEN))
                .thenReturn(List.of(serviceRequest));
        when(mapper.toSummaryResponse(eq(serviceRequest), anyDouble()))
                .thenReturn(new ServiceRequestSummaryResponse(
                        serviceRequest.getId(),
                        serviceRequest.getTitle(),
                        serviceRequest.getCategory(),
                        serviceRequest.getCity(),
                        serviceRequest.getDistrict(),
                        0.7,
                        serviceRequest.getBudgetMin(),
                        serviceRequest.getBudgetMax(),
                        serviceRequest.getPreferredDate(),
                        serviceRequest.getPreferredTime(),
                        false,
                        serviceRequest.getStatus(),
                        0,
                        serviceRequest.getCreatedAt()
                ));

        Page<ServiceRequestSummaryResponse> result = serviceRequestService.searchNearbyRequests(
                11.5500, 104.9000, 10.0, null, null, null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(0.7, result.getContent().get(0).distanceKm());
    }

    @Test
    void shouldReturnEmptyWhenRequestIsOutsideRadius() {
        // Request at 11.5435, 104.8997. Search at Siem Reap: 13.3633, 103.8564 (approx 230 km away)
        when(requestRepository.findByStatusAndHasCoordinates(ServiceRequestStatus.OPEN))
                .thenReturn(List.of(serviceRequest));

        Page<ServiceRequestSummaryResponse> result = serviceRequestService.searchNearbyRequests(
                13.3633, 103.8564, 10.0, null, null, null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldCreateOfferByProvider() {
        ServiceRequestOfferRequest request = new ServiceRequestOfferRequest(
                new BigDecimal("35.00"),
                "I can fix it tomorrow",
                "2 hours"
        );

        when(providerProfileRepository.findByUserId(providerUser.getId())).thenReturn(Optional.of(providerProfile));
        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(offerRepository.existsByServiceRequestIdAndProviderIdAndStatus(serviceRequest.getId(), providerProfile.getId(), ServiceOfferStatus.PENDING))
                .thenReturn(false);
        when(offerRepository.save(any(ServiceRequestOffer.class))).thenReturn(offer);
        when(mapper.toOfferResponse(offer)).thenReturn(mock(ServiceRequestOfferResponse.class));

        ServiceRequestOfferResponse response = serviceRequestService.createOffer(providerUser.getId(), serviceRequest.getId(), request);

        assertNotNull(response);
        verify(offerRepository).save(any(ServiceRequestOffer.class));
        verify(notificationService).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldPreventCustomerFromOfferingOnOwnRequest() {
        ServiceRequestOfferRequest request = new ServiceRequestOfferRequest(
                new BigDecimal("35.00"), "Message", "1 hour"
        );

        when(providerProfileRepository.findByUserId(customer.getId())).thenReturn(Optional.of(providerProfile));
        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () ->
                serviceRequestService.createOffer(customer.getId(), serviceRequest.getId(), request));
    }

    @Test
    void shouldAcceptOfferAndRejectOtherOffers() {
        ServiceRequestOffer otherOffer = ServiceRequestOffer.builder()
                .id(UUID.randomUUID())
                .serviceRequest(serviceRequest)
                .status(ServiceOfferStatus.PENDING)
                .build();

        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.findByServiceRequestId(serviceRequest.getId())).thenReturn(List.of(offer, otherOffer));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(mapper.toResponse(any(ServiceRequest.class), eq(customer.getId()), eq(UserRole.CUSTOMER), isNull()))
                .thenReturn(mock(ServiceRequestResponse.class));

        serviceRequestService.acceptOffer(customer.getId(), serviceRequest.getId(), offer.getId());

        assertEquals(ServiceRequestStatus.ACCEPTED, serviceRequest.getStatus());
        assertEquals(providerProfile, serviceRequest.getSelectedProvider());
        assertEquals(ServiceOfferStatus.ACCEPTED, offer.getStatus());
        assertEquals(ServiceOfferStatus.REJECTED, otherOffer.getStatus());
        verify(bookingRepository).save(any(Booking.class));
        verify(notificationService, atLeastOnce()).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectOfferByCustomer() {
        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(any(ServiceRequestOffer.class))).thenReturn(offer);
        when(mapper.toOfferResponse(offer)).thenReturn(mock(ServiceRequestOfferResponse.class));

        serviceRequestService.rejectOffer(customer.getId(), serviceRequest.getId(), offer.getId());

        assertEquals(ServiceOfferStatus.REJECTED, offer.getStatus());
        verify(notificationService).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldStartAndCompleteAcceptedServiceRequest() {
        serviceRequest.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequest.setSelectedProvider(providerProfile);

        when(requestRepository.findById(serviceRequest.getId())).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(mapper.toResponse(any(ServiceRequest.class), eq(providerUser.getId()), eq(UserRole.PROVIDER), isNull()))
                .thenReturn(mock(ServiceRequestResponse.class));

        serviceRequestService.startRequest(providerUser.getId(), serviceRequest.getId());
        assertEquals(ServiceRequestStatus.IN_PROGRESS, serviceRequest.getStatus());

        serviceRequestService.completeRequest(providerUser.getId(), serviceRequest.getId());
        assertEquals(ServiceRequestStatus.COMPLETED, serviceRequest.getStatus());
        verify(notificationService, atLeast(2)).sendNotification(any(), any(), any(), any(), any(), any());
    }
}
