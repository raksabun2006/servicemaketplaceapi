package com.kh.serviceplatform.features.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"booking", "customer", "provider"})
    Page<Review> findByProviderId(UUID providerId, Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "customer", "provider"})
    Optional<Review> findById(UUID id);

    boolean existsByBookingId(UUID bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.provider.id = :providerId")
    Double calculateAverageRatingByProviderId(@Param("providerId") UUID providerId);

    long countByProviderId(UUID providerId);
}
