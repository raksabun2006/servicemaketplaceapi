package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestOfferRepository extends JpaRepository<ServiceRequestOffer, UUID> {

    @EntityGraph(attributePaths = {"serviceRequest", "provider", "provider.user"})
    Page<ServiceRequestOffer> findByServiceRequestId(UUID serviceRequestId, Pageable pageable);

    @EntityGraph(attributePaths = {"serviceRequest", "provider", "provider.user"})
    List<ServiceRequestOffer> findByServiceRequestId(UUID serviceRequestId);

    @EntityGraph(attributePaths = {"serviceRequest", "provider", "provider.user"})
    Page<ServiceRequestOffer> findByProviderId(UUID providerId, Pageable pageable);

    @EntityGraph(attributePaths = {"serviceRequest", "provider", "provider.user"})
    Optional<ServiceRequestOffer> findByServiceRequestIdAndProviderId(UUID serviceRequestId, UUID providerId);

    @Override
    @EntityGraph(attributePaths = {"serviceRequest", "provider", "provider.user"})
    Optional<ServiceRequestOffer> findById(UUID id);

    boolean existsByServiceRequestIdAndProviderIdAndStatus(UUID serviceRequestId, UUID providerId, ServiceOfferStatus status);

    long countByServiceRequestId(UUID serviceRequestId);

    long countByProviderIdAndStatus(UUID providerId, ServiceOfferStatus status);
}
