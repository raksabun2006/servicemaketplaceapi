package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.service.ServiceOffer;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOffer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_customer_id", columnList = "customer_id"),
                @Index(name = "idx_bookings_provider_id", columnList = "provider_id"),
                @Index(name = "idx_bookings_service_id", columnList = "service_id"),
                @Index(name = "idx_bookings_request_id", columnList = "service_request_id"),
                @Index(name = "idx_bookings_offer_id", columnList = "accepted_offer_id"),
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceOffer service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_offer_id")
    private ServiceRequestOffer acceptedOffer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_start_time", length = 30)
    private String scheduledStartTime;

    @Column(name = "scheduled_end_time", length = 30)
    private String scheduledEndTime;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
