package com.kh.serviceplatform.features.file.dto;

import com.kh.serviceplatform.features.file.enums.FileType;

import java.util.UUID;

public record FileUploadResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long size,
        FileType fileType,
        String url
) {
}
