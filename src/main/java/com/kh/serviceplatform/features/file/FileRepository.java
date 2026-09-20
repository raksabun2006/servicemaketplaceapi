package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.features.file.enums.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<StoredFile, UUID> {

    @EntityGraph(attributePaths = {"owner"})
    Optional<StoredFile> findById(UUID id);

    @EntityGraph(attributePaths = {"owner"})
    Optional<StoredFile> findByStorageKey(String storageKey);

    @EntityGraph(attributePaths = {"owner"})
    Page<StoredFile> findByOwnerId(UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    List<StoredFile> findByOwnerIdAndFileType(UUID ownerId, FileType fileType);
}
