package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    @Override
    @EntityGraph(attributePaths = {"avatarFile"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"avatarFile"})
    Optional<User> findById(UUID id);

    long countByRole(UserRole role);

    long countByStatus(UserStatus status);
}
