package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.features.file.dto.FileResponse;
import com.kh.serviceplatform.features.file.dto.FileUploadResponse;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    public FileUploadResponse toUploadResponse(StoredFile file) {
        if (file == null) {
            return null;
        }

        return new FileUploadResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getFileSize(),
                file.getFileType(),
                "/api/v1/files/" + file.getId()
        );
    }

    public FileResponse toResponse(StoredFile file) {
        if (file == null) {
            return null;
        }

        return new FileResponse(
                file.getId(),
                file.getOwner() != null ? file.getOwner().getId() : null,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getFileSize(),
                file.getFileType(),
                "/api/v1/files/" + file.getId(),
                file.getCreatedAt()
        );
    }
}
