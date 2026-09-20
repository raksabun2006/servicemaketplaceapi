package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "service_area", length = 150)
    private String serviceArea;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Builder.Default
    @Column(name = "service_radius_km")
    private Double serviceRadiusKm = 10.0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", length = 20)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Builder.Default
    @Column(name = "working_days", length = 255)
    private String workingDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";

    @Builder.Default
    @Column(name = "working_hours_start", length = 10)
    private String workingHoursStart = "08:00";

    @Builder.Default
    @Column(name = "working_hours_end", length = 10)
    private String workingHoursEnd = "18:00";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identity_document_file_id")
    private StoredFile identityDocumentFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_photo_file_id")
    private StoredFile profilePhotoFile;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 30)
    private ProviderVerificationStatus verificationStatus = ProviderVerificationStatus.UNVERIFIED;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Builder.Default
    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Builder.Default
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Builder.Default
    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Builder.Default
    @Column(name = "completed_services")
    private Integer completedServices = 0;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void ensureConsistency() {
        if (this.verificationStatus == null) {
            this.verificationStatus = Boolean.TRUE.equals(this.isVerified) ? ProviderVerificationStatus.VERIFIED : ProviderVerificationStatus.UNVERIFIED;
        }
        if (this.availabilityStatus == null) {
            this.availabilityStatus = Boolean.TRUE.equals(this.isAvailable) ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.OFFLINE;
        }
        this.isAvailable = (this.availabilityStatus == AvailabilityStatus.AVAILABLE);
        this.isVerified = (this.verificationStatus == ProviderVerificationStatus.VERIFIED);
        if (this.averageRating == null) {
            this.averageRating = 0.0;
        }
        if (this.totalReviews == null) {
            this.totalReviews = 0;
        }
        if (this.completedServices == null) {
            this.completedServices = 0;
        }
        if (this.serviceRadiusKm == null) {
            this.serviceRadiusKm = 10.0;
        }
    }

    public boolean isAvailable() {
        return this.isAvailable == null || this.isAvailable;
    }

    public void setAvailable(Boolean available) {
        this.isAvailable = available;
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);
    }

    public void setVerified(Boolean verified) {
        this.isVerified = verified;
    }

    public Double getAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }

    public Integer getTotalReviews() {
        return totalReviews != null ? totalReviews : 0;
    }

    public Integer getCompletedServices() {
        return completedServices != null ? completedServices : 0;
    }

    public ProviderVerificationStatus getVerificationStatus() {
        if (this.verificationStatus == null) {
            return Boolean.TRUE.equals(this.isVerified) ? ProviderVerificationStatus.VERIFIED : ProviderVerificationStatus.UNVERIFIED;
        }
        return this.verificationStatus;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        if (this.availabilityStatus == null) {
            return this.isAvailable() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.OFFLINE;
        }
        return this.availabilityStatus;
    }
}
