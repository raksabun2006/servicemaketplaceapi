package com.kh.serviceplatform.features.customer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Optional<CustomerProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
