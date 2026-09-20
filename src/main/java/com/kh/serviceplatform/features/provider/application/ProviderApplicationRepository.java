package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProviderApplicationRepository extends JpaRepository<ProviderApplication, UUID>, JpaSpecificationExecutor<ProviderApplication> {

    @Override
    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile", "reviewedBy"})
    Page<ProviderApplication> findAll(Specification<ProviderApplication> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile", "reviewedBy"})
    Optional<ProviderApplication> findById(UUID id);

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile", "reviewedBy"})
    Optional<ProviderApplication> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndApplicationStatus(UUID userId, ProviderApplicationStatus status);

    @EntityGraph(attributePaths = {"user", "identityDocumentFile", "profilePhotoFile", "reviewedBy"})
    Page<ProviderApplication> findByApplicationStatus(ProviderApplicationStatus status, Pageable pageable);
}
