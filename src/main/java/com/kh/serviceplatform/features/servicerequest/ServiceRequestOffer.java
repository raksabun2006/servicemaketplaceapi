package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "service_request_offers",
        indexes = {
                @Index(name = "idx_sro_service_request_id", columnList = "service_request_id"),
                @Index(name = "idx_sro_provider_id", columnList = "provider_id"),
                @Index(name = "idx_sro_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @Column(name = "proposed_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "estimated_completion_time", length = 100)
    private String estimatedCompletionTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceOfferStatus status = ServiceOfferStatus.PENDING;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
