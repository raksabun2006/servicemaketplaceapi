package com.kh.serviceplatform.features.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceOffer, UUID>, JpaSpecificationExecutor<ServiceOffer> {

    @Override
    @EntityGraph(attributePaths = {"provider", "provider.user", "imageFile"})
    Page<ServiceOffer> findAll(Specification<ServiceOffer> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"provider", "provider.user", "imageFile"})
    Optional<ServiceOffer> findById(UUID id);

    @EntityGraph(attributePaths = {"provider", "provider.user", "imageFile"})
    Page<ServiceOffer> findByProviderId(UUID providerId, Pageable pageable);

    @EntityGraph(attributePaths = {"provider", "provider.user", "imageFile"})
    Page<ServiceOffer> findByProviderIdAndIsAvailableTrue(UUID providerId, Pageable pageable);

    long countByProviderId(UUID providerId);
}
