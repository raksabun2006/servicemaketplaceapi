package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

@Entity
@Table(
        name = "service_requests",
        indexes = {
                @Index(name = "idx_sr_customer_id", columnList = "customer_id"),
                @Index(name = "idx_sr_selected_provider_id", columnList = "selected_provider_id"),
                @Index(name = "idx_sr_status", columnList = "status"),
                @Index(name = "idx_sr_category", columnList = "category"),
                @Index(name = "idx_sr_city", columnList = "city"),
                @Index(name = "idx_sr_district", columnList = "district"),
                @Index(name = "idx_sr_created_at", columnList = "created_at"),
                @Index(name = "idx_sr_urgent", columnList = "is_urgent"),
                @Index(name = "idx_sr_location", columnList = "latitude, longitude")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_provider_id")
    private ProviderProfile selectedProvider;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceCategory category;

    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "preferred_time", length = 30)
    private String preferredTime;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Builder.Default
    @Column(name = "is_urgent")
    private Boolean urgent = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestStatus status = ServiceRequestStatus.OPEN;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "service_request_images",
            joinColumns = @JoinColumn(name = "service_request_id"),
            inverseJoinColumns = @JoinColumn(name = "file_id")
    )
    @Builder.Default
    private Set<StoredFile> images = new LinkedHashSet<>();

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ServiceRequestOffer> offers = new LinkedHashSet<>();

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public boolean isUrgent() {
        return Boolean.TRUE.equals(this.urgent);
    }
}
