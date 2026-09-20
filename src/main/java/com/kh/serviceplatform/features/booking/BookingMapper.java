package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.booking.dto.BookingResponse;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.service.ServiceOffer;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOffer;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        User customer = booking.getCustomer();
        ProviderProfile provider = booking.getProvider();
        ServiceOffer service = booking.getService();
        ServiceRequest serviceRequest = booking.getServiceRequest();
        ServiceRequestOffer acceptedOffer = booking.getAcceptedOffer();

        String providerFullName = provider != null && provider.getUser() != null ? provider.getUser().getFullName() : null;
        String providerPhone = provider != null && provider.getUser() != null ? provider.getUser().getPhone() : null;

        return new BookingResponse(
                booking.getId(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getEmail() : null,
                customer != null ? customer.getPhone() : null,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getBusinessName() : null,
                providerFullName,
                providerPhone,
                service != null ? service.getId() : null,
                service != null ? service.getName() : null,
                serviceRequest != null ? serviceRequest.getId() : null,
                serviceRequest != null ? serviceRequest.getTitle() : null,
                acceptedOffer != null ? acceptedOffer.getId() : null,
                booking.getPrice(),
                booking.getStatus(),
                booking.getAddress(),
                booking.getCity(),
                booking.getScheduledDate(),
                booking.getScheduledStartTime(),
                booking.getScheduledEndTime(),
                booking.getScheduledAt(),
                booking.getNotes(),
                booking.getCancellationReason(),
                booking.getRejectionReason(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
