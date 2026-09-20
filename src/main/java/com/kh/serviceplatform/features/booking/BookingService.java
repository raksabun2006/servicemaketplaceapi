package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.booking.dto.BookingResponse;
import com.kh.serviceplatform.features.booking.dto.CancelBookingRequest;
import com.kh.serviceplatform.features.booking.dto.CreateBookingRequest;
import com.kh.serviceplatform.features.booking.dto.RejectBookingRequest;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(UUID customerId, CreateBookingRequest request);

    Page<BookingResponse> getBookings(UUID userId, UserRole role, BookingStatus status, Pageable pageable);

    BookingResponse getBookingById(UUID userId, UserRole role, UUID bookingId);

    BookingResponse cancelBooking(UUID userId, UserRole role, UUID bookingId, CancelBookingRequest request);

    BookingResponse acceptBooking(UUID providerUserId, UUID bookingId);

    BookingResponse rejectBooking(UUID providerUserId, UUID bookingId, RejectBookingRequest request);

    BookingResponse startBooking(UUID providerUserId, UUID bookingId);

    BookingResponse completeBooking(UUID providerUserId, UUID bookingId);
}
