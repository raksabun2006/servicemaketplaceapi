package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"customer", "provider", "provider.user", "service", "serviceRequest", "acceptedOffer"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "provider", "provider.user", "service", "serviceRequest", "acceptedOffer"})
    Optional<Booking> findById(UUID id);

    @EntityGraph(attributePaths = {"customer", "provider", "provider.user", "service", "serviceRequest", "acceptedOffer"})
    Page<Booking> findByCustomerId(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "provider", "provider.user", "service", "serviceRequest", "acceptedOffer"})
    Page<Booking> findByProviderId(UUID providerId, Pageable pageable);

    long countByStatus(BookingStatus status);

    long countByCustomerId(UUID customerId);

    long countByProviderId(UUID providerId);

    long countByProviderIdAndStatus(UUID providerId, BookingStatus status);

    long countByProviderIdAndStatusIn(UUID providerId, Collection<BookingStatus> statuses);

    long countByCustomerIdAndStatus(UUID customerId, BookingStatus status);

    long countByCustomerIdAndStatusIn(UUID customerId, Collection<BookingStatus> statuses);

    long countByProviderIdAndScheduledDate(UUID providerId, LocalDate scheduledDate);

    @Query("SELECT COALESCE(SUM(b.price), 0.00) FROM Booking b WHERE b.status = :status")
    BigDecimal sumPriceByStatus(@Param("status") BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.price), 0.00) FROM Booking b WHERE b.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();
}
