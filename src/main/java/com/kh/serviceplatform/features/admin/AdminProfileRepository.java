package com.kh.serviceplatform.features.admin;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Optional<AdminProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
