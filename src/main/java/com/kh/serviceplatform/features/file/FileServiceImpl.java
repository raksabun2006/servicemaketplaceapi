package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.file.dto.FileResponse;
import com.kh.serviceplatform.features.file.dto.FileUploadResponse;
import com.kh.serviceplatform.features.file.enums.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileContentValidator fileContentValidator;
    private final FileMapper fileMapper;

    // =========================================================
    // Upload
    // =========================================================

    @Override
    public FileUploadResponse uploadFile(
            MultipartFile file,
            FileType fileType,
            UUID currentUserId
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (fileType == null) {
            throw new BadRequestException("FileType parameter is required");
        }

        if (currentUserId == null) {
            throw new ForbiddenException(
                    "Authentication required to upload file"
            );
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with ID: " + currentUserId
                        )
                );

        // 1. Role-based upload permission
        validateRoleUploadPermission(user, fileType);

        // 2. File size
        validateFileSize(file, fileType);

        // 3. Validate file content / magic bytes
        String detectedContentType =
                fileContentValidator.validateAndDetectContentType(
                        file,
                        fileType
                );

        // 4. Sanitize filename
        String originalFilename =
                FileContentValidator.sanitizeFilename(
                        file.getOriginalFilename()
                );

        String extension =
                FileContentValidator.getExtension(originalFilename);

        String randomFilename =
                UUID.randomUUID()
                        + (extension.isEmpty()
                        ? ""
                        : "." + extension);

        String targetDirectory =
                fileType.getDirectory() + "/" + currentUserId;

        // 5. Store physical file
        String storageKey =
                fileStorageService.upload(
                        file,
                        targetDirectory,
                        randomFilename
                );

        // 6. Store metadata
        StoredFile storedFile = StoredFile.builder()
                .owner(user)
                .originalFilename(originalFilename)
                .storedFilename(randomFilename)
                .storageKey(storageKey)
                .contentType(detectedContentType)
                .fileSize(file.getSize())
                .fileType(fileType)
                .build();

        StoredFile savedFile =
                fileRepository.save(storedFile);

        // 7. Automatically set avatar
        if (fileType == FileType.AVATAR) {
            user.setAvatarFile(savedFile);
            userRepository.save(user);
        }

        log.info(
                "File uploaded successfully. FileId: {}, UserId: {}, Type: {}, Size: {}",
                savedFile.getId(),
                currentUserId,
                fileType,
                file.getSize()
        );

        return fileMapper.toUploadResponse(savedFile);
    }

    // =========================================================
    // Download by ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(
            UUID fileId,
            UUID currentUserId
    ) {
        StoredFile storedFile =
                getFileAndValidateAccess(
                        fileId,
                        currentUserId
                );

        return fileStorageService.download(
                storedFile.getStorageKey()
        );
    }

    // =========================================================
    // Download by storage key
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFileByStorageKey(
            String storageKey,
            UUID currentUserId
    ) {
        StoredFile storedFile =
                getFileByStorageKeyAndValidateAccess(
                        storageKey,
                        currentUserId
                );

        return fileStorageService.download(
                storedFile.getStorageKey()
        );
    }

    // =========================================================
    // Download by filename
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFileByFilename(
            String filename,
            UUID currentUserId
    ) {
        StoredFile storedFile =
                getFileByFilenameAndValidateAccess(
                        filename,
                        currentUserId
                );

        return fileStorageService.download(
                storedFile.getStorageKey()
        );
    }

    // =========================================================
    // Metadata by ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public StoredFile getFileMetadata(
            UUID fileId,
            UUID currentUserId
    ) {
        return getFileAndValidateAccess(
                fileId,
                currentUserId
        );
    }

    // =========================================================
    // Metadata by storage key
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public StoredFile getFileMetadataByStorageKey(
            String storageKey,
            UUID currentUserId
    ) {
        return getFileByStorageKeyAndValidateAccess(
                storageKey,
                currentUserId
        );
    }

    // =========================================================
    // Metadata by filename
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public StoredFile getFileMetadataByFilename(
            String filename,
            UUID currentUserId
    ) {
        return getFileByFilenameAndValidateAccess(
                filename,
                currentUserId
        );
    }

    // =========================================================
    // File information
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileInfo(
            UUID fileId,
            UUID currentUserId
    ) {
        StoredFile storedFile =
                getFileAndValidateAccess(
                        fileId,
                        currentUserId
                );

        return fileMapper.toResponse(storedFile);
    }

    // =========================================================
    // Delete
    // =========================================================

    @Override
    public void deleteFile(
            UUID fileId,
            UUID currentUserId
    ) {
        if (currentUserId == null) {
            throw new ForbiddenException(
                    "Authentication required to delete file"
            );
        }

        StoredFile storedFile =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "File not found with ID: " + fileId
                                )
                        );

        User currentUser =
                userRepository.findById(currentUserId)
                        .orElseThrow(() ->
                                new ForbiddenException(
                                        "User not found"
                                )
                        );

        // Only owner or ADMIN can delete
        boolean isOwner =
                storedFile.getOwner()
                        .getId()
                        .equals(currentUserId);

        boolean isAdmin =
                currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException(
                    "You do not have permission to delete this file"
            );
        }

        // Delete physical file
        fileStorageService.delete(
                storedFile.getStorageKey()
        );

        // Delete database record
        fileRepository.delete(storedFile);

        log.info(
                "File deleted successfully. FileId: {}, DeletedBy: {}",
                fileId,
                currentUserId
        );
    }

    // =========================================================
    // Find file by ID + validate access
    // =========================================================

    private StoredFile getFileAndValidateAccess(
            UUID fileId,
            UUID currentUserId
    ) {
        StoredFile storedFile =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "File not found with ID: " + fileId
                                )
                        );

        return validateAccess(
                storedFile,
                currentUserId
        );
    }

    // =========================================================
    // Find file by storage key + validate access
    // =========================================================

    private StoredFile getFileByStorageKeyAndValidateAccess(
            String storageKey,
            UUID currentUserId
    ) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new BadRequestException(
                    "Storage key is required"
            );
        }

        StoredFile storedFile =
                fileRepository.findByStorageKey(storageKey)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "File not found with storage key: "
                                                + storageKey
                                )
                        );

        return validateAccess(
                storedFile,
                currentUserId
        );
    }

    // =========================================================
    // Find file by filename + validate access
    // =========================================================

    private StoredFile getFileByFilenameAndValidateAccess(
            String filename,
            UUID currentUserId
    ) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Filename is required");
        }

        StoredFile storedFile = fileRepository.findByStoredFilename(filename)
                .orElseGet(() -> fileRepository.findByStorageKey(filename)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("File not found with filename: " + filename)));

        return validateAccess(storedFile, currentUserId);
    }

    // =========================================================
    // Access validation
    // =========================================================

    private StoredFile validateAccess(
            StoredFile storedFile,
            UUID currentUserId
    ) {
        /*
         * =====================================================
         * PUBLIC FILES & IMAGES
         * =====================================================
         *
         * Visitors without an account can view:
         * - Avatars
         * - Service images
         * - Portfolio images
         * - Request images
         * - Category icons
         * - Any other image file (except sensitive PROVIDER_DOCUMENT)
         */
        if (isPublicFile(storedFile)) {
            return storedFile;
        }

        /*
         * =====================================================
         * PROTECTED FILES
         * =====================================================
         *
         * PROVIDER_DOCUMENT (e.g. government ID) and sensitive files
         * strictly require authentication.
         */
        if (currentUserId == null) {
            throw new ForbiddenException(
                    "Authentication required to access this file"
            );
        }

        User currentUser =
                userRepository.findById(currentUserId)
                        .orElseThrow(() ->
                                new ForbiddenException(
                                        "User not found"
                                )
                        );

        /*
         * ADMIN can access protected files.
         */
        if (currentUser.getRole() == UserRole.ADMIN) {
            return storedFile;
        }

        /*
         * Owner can access their own protected files.
         */
        if (storedFile.getOwner() != null &&
                storedFile.getOwner().getId().equals(currentUserId)) {
            return storedFile;
        }

        throw new ForbiddenException(
                "You do not have permission to access this file"
        );
    }

    // =========================================================
    // Public file types check
    // =========================================================

    private boolean isPublicFile(StoredFile storedFile) {
        if (storedFile == null) {
            return false;
        }

        FileType type = storedFile.getFileType();

        // Sensitive provider identity documents remain protected
        if (type == FileType.PROVIDER_DOCUMENT) {
            return false;
        }

        // Standard marketplace media types are public
        if (type == FileType.AVATAR
                || type == FileType.SERVICE_IMAGE
                || type == FileType.PORTFOLIO_IMAGE
                || type == FileType.REQUEST_IMAGE
                || type == FileType.CATEGORY_ICON) {
            return true;
        }

        // Any image content type is viewable publicly
        return storedFile.getContentType() != null
                && storedFile.getContentType().toLowerCase().startsWith("image/");
    }

    // =========================================================
    // Upload permission
    // =========================================================

    private void validateRoleUploadPermission(
            User user,
            FileType fileType
    ) {
        UserRole role = user.getRole();

        switch (role) {

            case CUSTOMER -> {
                if (fileType != FileType.AVATAR
                        && fileType != FileType.REQUEST_IMAGE
                        && fileType != FileType.PROVIDER_DOCUMENT) {

                    throw new ForbiddenException(
                            "CUSTOMER role is not allowed to upload "
                                    + fileType
                    );
                }
            }

            case PROVIDER -> {
                if (fileType != FileType.AVATAR
                        && fileType != FileType.PROVIDER_DOCUMENT
                        && fileType != FileType.SERVICE_IMAGE
                        && fileType != FileType.PORTFOLIO_IMAGE
                        && fileType != FileType.REQUEST_IMAGE) {

                    throw new ForbiddenException(
                            "PROVIDER role is not allowed to upload "
                                    + fileType
                    );
                }
            }

            case ADMIN -> {
                // ADMIN can upload any file type
            }
        }
    }

    // =========================================================
    // File size validation
    // =========================================================

    private void validateFileSize(
            MultipartFile file,
            FileType fileType
    ) {
        long size = file.getSize();

        long maxBytes = switch (fileType) {

            case AVATAR, CATEGORY_ICON ->
                    5L * 1024 * 1024;

            case PROVIDER_DOCUMENT,
                 SERVICE_IMAGE,
                 REQUEST_IMAGE,
                 PORTFOLIO_IMAGE,
                 OTHER ->
                    10L * 1024 * 1024;
        };

        if (size > maxBytes) {
            throw new BadRequestException(
                    "File size ("
                            + (size / (1024 * 1024))
                            + " MB) exceeds the maximum allowed limit for "
                            + fileType
                            + " ("
                            + (maxBytes / (1024 * 1024))
                            + " MB)"
            );
        }
    }
}
