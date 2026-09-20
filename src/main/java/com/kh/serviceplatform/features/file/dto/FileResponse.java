package com.kh.serviceplatform.features.file.dto;

import com.kh.serviceplatform.features.file.enums.FileType;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        UUID ownerId,
        String originalFilename,
        String contentType,
        long size,
        FileType fileType,
        String url,
        Instant createdAt
) {
}
