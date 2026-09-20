package com.kh.serviceplatform.features.favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteProviderRepository extends JpaRepository<FavoriteProvider, UUID> {

    boolean existsByCustomerIdAndProviderId(UUID customerId, UUID providerId);

    Optional<FavoriteProvider> findByCustomerIdAndProviderId(UUID customerId, UUID providerId);

    void deleteByCustomerIdAndProviderId(UUID customerId, UUID providerId);

    @Query("SELECT f FROM FavoriteProvider f JOIN FETCH f.provider p JOIN FETCH p.user u WHERE f.customer.id = :customerId")
    Page<FavoriteProvider> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    long countByCustomerId(UUID customerId);
}
