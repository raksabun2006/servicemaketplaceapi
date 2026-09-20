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

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, FileType fileType, UUID currentUserId) {
        if (fileType == null) {
            throw new BadRequestException("FileType parameter is required");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        // 1. Role-based upload permission validation
        validateRoleUploadPermission(user, fileType);

        // 2. File size validation
        validateFileSize(file, fileType);

        // 3. File content and extension validation (magic bytes)
        String detectedContentType = fileContentValidator.validateAndDetectContentType(file, fileType);

        String originalFilename = FileContentValidator.sanitizeFilename(file.getOriginalFilename());
        String extension = FileContentValidator.getExtension(originalFilename);
        String randomFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String targetDirectory = fileType.getDirectory() + "/" + currentUserId;

        // 4. Store physical file
        String storageKey = fileStorageService.upload(file, targetDirectory, randomFilename);

        // 5. Persist metadata
        StoredFile storedFile = StoredFile.builder()
                .owner(user)
                .originalFilename(originalFilename)
                .storedFilename(randomFilename)
                .storageKey(storageKey)
                .contentType(detectedContentType)
                .fileSize(file.getSize())
                .fileType(fileType)
                .build();

        StoredFile savedFile = fileRepository.save(storedFile);

        // 6. Profile Avatar auto-integration
        if (fileType == FileType.AVATAR) {
            user.setAvatarFile(savedFile);
            userRepository.save(user);
        }

        log.info("File uploaded successfully. FileId: {}, UserId: {}, Type: {}, Size: {}",
                savedFile.getId(), currentUserId, fileType, file.getSize());

        return fileMapper.toUploadResponse(savedFile);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(UUID fileId, UUID currentUserId) {
        StoredFile storedFile = getFileAndValidateAccess(fileId, currentUserId);
        return fileStorageService.download(storedFile.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFileByStorageKey(String storageKey, UUID currentUserId) {
        StoredFile storedFile = getFileByStorageKeyAndValidateAccess(storageKey, currentUserId);
        return fileStorageService.download(storedFile.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getFileMetadata(UUID fileId, UUID currentUserId) {
        return getFileAndValidateAccess(fileId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getFileMetadataByStorageKey(String storageKey, UUID currentUserId) {
        return getFileByStorageKeyAndValidateAccess(storageKey, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileInfo(UUID fileId, UUID currentUserId) {
        StoredFile storedFile = getFileAndValidateAccess(fileId, currentUserId);
        return fileMapper.toResponse(storedFile);
    }

    @Override
    public void deleteFile(UUID fileId, UUID currentUserId) {
        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with ID: " + fileId));

        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required to delete file");
        }

        // Only owner or ADMIN can delete
        if (!storedFile.getOwner().getId().equals(currentUserId) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("You do not have permission to delete this file");
        }

        fileStorageService.delete(storedFile.getStorageKey());
        fileRepository.delete(storedFile);

        log.info("File deleted successfully. FileId: {}, DeletedBy: {}", fileId, currentUserId);
    }

    private StoredFile getFileAndValidateAccess(UUID fileId, UUID currentUserId) {
        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with ID: " + fileId));
        return validateAccess(storedFile, currentUserId);
    }

    private StoredFile getFileByStorageKeyAndValidateAccess(String storageKey, UUID currentUserId) {
        StoredFile storedFile = fileRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with storage key: " + storageKey));
        return validateAccess(storedFile, currentUserId);
    }

    private StoredFile validateAccess(StoredFile storedFile, UUID currentUserId) {
        FileType type = storedFile.getFileType();

        // Publicly readable types (avatars, service catalog images, portfolio images, customer request images, category icons)
        if (type == FileType.AVATAR || type == FileType.SERVICE_IMAGE || type == FileType.PORTFOLIO_IMAGE || type == FileType.REQUEST_IMAGE || type == FileType.CATEGORY_ICON) {
            return storedFile;
        }

        // Sensitive / Protected types (such as verification documents) require authentication
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required to access this file");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        if (currentUser.getRole() == UserRole.ADMIN) {
            return storedFile;
        }

        if (storedFile.getOwner().getId().equals(currentUserId)) {
            return storedFile;
        }

        throw new ForbiddenException("You do not have permission to access this file");
    }

    private void validateRoleUploadPermission(User user, FileType fileType) {
        UserRole role = user.getRole();
        switch (role) {
            case CUSTOMER -> {
                if (fileType != FileType.AVATAR && fileType != FileType.REQUEST_IMAGE && fileType != FileType.PROVIDER_DOCUMENT) {
                    throw new ForbiddenException("CUSTOMER role is not allowed to upload " + fileType);
                }
            }
            case PROVIDER -> {
                if (fileType != FileType.AVATAR &&
                        fileType != FileType.PROVIDER_DOCUMENT &&
                        fileType != FileType.SERVICE_IMAGE &&
                        fileType != FileType.PORTFOLIO_IMAGE &&
                        fileType != FileType.REQUEST_IMAGE) {
                    throw new ForbiddenException("PROVIDER role is not allowed to upload " + fileType);
                }
            }
            case ADMIN -> {
                // ADMIN can upload any file type
            }
        }
    }

    private void validateFileSize(MultipartFile file, FileType fileType) {
        long size = file.getSize();
        long maxBytes = switch (fileType) {
            case AVATAR, CATEGORY_ICON -> 5L * 1024 * 1024; // 5 MB
            case PROVIDER_DOCUMENT, SERVICE_IMAGE, REQUEST_IMAGE, PORTFOLIO_IMAGE, OTHER -> 10L * 1024 * 1024; // 10 MB
        };

        if (size > maxBytes) {
            throw new BadRequestException("File size (" + (size / (1024 * 1024)) + " MB) exceeds the maximum allowed limit for "
                    + fileType + " (" + (maxBytes / (1024 * 1024)) + " MB)");
        }
    }
}
