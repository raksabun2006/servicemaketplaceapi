package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
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

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID>, JpaSpecificationExecutor<ServiceRequest> {

    @Override
    @EntityGraph(attributePaths = {"customer", "selectedProvider", "selectedProvider.user", "images", "offers"})
    Page<ServiceRequest> findAll(Specification<ServiceRequest> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer", "selectedProvider", "selectedProvider.user", "images", "offers"})
    Optional<ServiceRequest> findById(UUID id);

    @EntityGraph(attributePaths = {"customer", "selectedProvider", "images", "offers"})
    Page<ServiceRequest> findByCustomerId(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "selectedProvider", "images", "offers"})
    Page<ServiceRequest> findByStatus(ServiceRequestStatus status, Pageable pageable);

    @Query("SELECT sr FROM ServiceRequest sr " +
            "LEFT JOIN FETCH sr.customer " +
            "LEFT JOIN FETCH sr.selectedProvider " +
            "WHERE sr.status = :status AND sr.latitude IS NOT NULL AND sr.longitude IS NOT NULL")
    List<ServiceRequest> findByStatusAndHasCoordinates(@Param("status") ServiceRequestStatus status);

    long countByStatus(ServiceRequestStatus status);

    long countByCustomerId(UUID customerId);

    long countByCustomerIdAndStatus(UUID customerId, ServiceRequestStatus status);

    long countBySelectedProviderIdAndStatus(UUID providerId, ServiceRequestStatus status);

    @Query("SELECT sr.category, COUNT(sr) FROM ServiceRequest sr GROUP BY sr.category ORDER BY COUNT(sr) DESC")
    List<Object[]> countRequestsByCategory();

    @Query("SELECT sr.city, COUNT(sr) FROM ServiceRequest sr WHERE sr.city IS NOT NULL AND sr.city <> '' GROUP BY sr.city ORDER BY COUNT(sr) DESC")
    List<Object[]> countRequestsByCity();
}
