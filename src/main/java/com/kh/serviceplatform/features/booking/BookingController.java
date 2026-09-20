package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.booking.dto.BookingResponse;
import com.kh.serviceplatform.features.booking.dto.CancelBookingRequest;
import com.kh.serviceplatform.features.booking.dto.CreateBookingRequest;
import com.kh.serviceplatform.features.booking.dto.RejectBookingRequest;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
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

@Tag(name = "Bookings", description = "Endpoints for creating, viewing, managing, and updating service bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a new booking", description = "Book a service offering as a customer",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Service unavailable or invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return bookingService.createBooking(currentUserId, request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List bookings", description = "List bookings for current user (filtered by Customer, Provider, or Admin role)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of bookings retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Page<BookingResponse> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();
        return bookingService.getBookings(currentUserId, currentRole, status, pageable);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking by ID", description = "Retrieve details for a specific booking owned or assigned to current user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking details found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not your booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public BookingResponse getBookingById(@PathVariable UUID bookingId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();
        return bookingService.getBookingById(currentUserId, currentRole, bookingId);
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel booking", description = "Cancel a PENDING or ACCEPTED booking by customer or assigned provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel booking in current status"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not your booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public BookingResponse cancelBooking(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();
        return bookingService.cancelBooking(currentUserId, currentRole, bookingId, request);
    }

    @PostMapping("/{bookingId}/accept")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Accept booking", description = "Accept a PENDING booking by assigned provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking state transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not assigned provider"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public BookingResponse acceptBooking(@PathVariable UUID bookingId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return bookingService.acceptBooking(currentUserId, bookingId);
    }

    @PostMapping("/{bookingId}/reject")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Reject booking", description = "Reject a PENDING booking by assigned provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking state transition or missing reason"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not assigned provider"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public BookingResponse rejectBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RejectBookingRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return bookingService.rejectBooking(currentUserId, bookingId, request);
    }

    @PostMapping("/{bookingId}/start")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Start service booking", description = "Mark an ACCEPTED booking as IN_PROGRESS by assigned provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking state transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not assigned provider"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public BookingResponse startBooking(@PathVariable UUID bookingId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return bookingService.startBooking(currentUserId, bookingId);
    }

    @PostMapping("/{bookingId}/complete")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Complete service booking", description = "Mark an IN_PROGRESS booking as COMPLETED by assigned provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking state transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not assigned provider"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public BookingResponse completeBooking(@PathVariable UUID bookingId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return bookingService.completeBooking(currentUserId, bookingId);
    }
}
