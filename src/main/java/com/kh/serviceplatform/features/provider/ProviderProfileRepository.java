package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID>, JpaSpecificationExecutor<ProviderProfile> {

    @Override
    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile"})
    Page<ProviderProfile> findAll(Specification<ProviderProfile> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile"})
    Optional<ProviderProfile> findById(UUID id);

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile"})
    Optional<ProviderProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile"})
    Page<ProviderProfile> findByIsAvailableTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile"})
    Page<ProviderProfile> findByAvailabilityStatus(AvailabilityStatus status, Pageable pageable);

    @Query("SELECT p FROM ProviderProfile p LEFT JOIN FETCH p.user WHERE p.isAvailable = true AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    List<ProviderProfile> findAvailableWithCoordinates();

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile"})
    Page<ProviderProfile> findByVerificationStatus(ProviderVerificationStatus status, Pageable pageable);

    long countByVerificationStatus(ProviderVerificationStatus status);

    long countByIsVerifiedTrue();

    long countByIsAvailableTrue();

    @Query("SELECT COALESCE(AVG(p.averageRating), 0.0) FROM ProviderProfile p WHERE p.totalReviews > 0")
    Double getAverageMarketplaceRating();

    @Query("SELECT p.city, COUNT(p) FROM ProviderProfile p WHERE p.city IS NOT NULL AND p.city <> '' GROUP BY p.city ORDER BY COUNT(p) DESC")
    List<Object[]> countProvidersByCity();
}
