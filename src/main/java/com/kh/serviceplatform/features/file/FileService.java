package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.features.file.dto.FileResponse;
import com.kh.serviceplatform.features.file.dto.FileUploadResponse;
import com.kh.serviceplatform.features.file.enums.FileType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileService {

    FileUploadResponse uploadFile(MultipartFile file, FileType fileType, UUID currentUserId);

    Resource downloadFile(UUID fileId, UUID currentUserId);

    Resource downloadFileByStorageKey(String storageKey, UUID currentUserId);

    StoredFile getFileMetadata(UUID fileId, UUID currentUserId);

    StoredFile getFileMetadataByStorageKey(String storageKey, UUID currentUserId);

    FileResponse getFileInfo(UUID fileId, UUID currentUserId);

    void deleteFile(UUID fileId, UUID currentUserId);
}
