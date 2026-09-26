package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.file.dto.FileResponse;
import com.kh.serviceplatform.features.file.dto.FileUploadResponse;
import com.kh.serviceplatform.features.file.enums.FileType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Files", description = "Secure file management and upload endpoints")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a file with role-based validation")
    public FileUploadResponse uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return fileService.uploadFile(file, fileType, currentUserId);
    }

    @GetMapping("/{fileId:[0-9a-fA-F\\-]{36}(?:\\.[a-zA-Z0-9]+)?}")
    @Operation(summary = "Download or stream a file by file ID (with optional extension)")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        String cleanId = fileId.contains(".") ? fileId.substring(0, fileId.indexOf('.')) : fileId;
        UUID parsedId = UUID.fromString(cleanId);
        UUID currentUserId = getOptionalCurrentUserId();

        StoredFile metadata = fileService.getFileMetadata(parsedId, currentUserId);
        Resource resource = fileService.downloadFile(parsedId, currentUserId);

        return buildFileResponse(metadata, resource);
    }

    @GetMapping("/filename/{filename}")
    @Operation(summary = "Download or stream a file by stored filename")
    public ResponseEntity<Resource> downloadFileByFilename(@PathVariable String filename) {
        UUID currentUserId = getOptionalCurrentUserId();
        StoredFile metadata = fileService.getFileMetadataByFilename(filename, currentUserId);
        Resource resource = fileService.downloadFileByFilename(filename, currentUserId);
        return buildFileResponse(metadata, resource);
    }

    @GetMapping("/{directory}/{userId}/{filename}")
    @Operation(summary = "Download or stream a file by storage path")
    public ResponseEntity<Resource> downloadFileByPath(
            @PathVariable String directory,
            @PathVariable String userId,
            @PathVariable String filename
    ) {
        String storageKey = directory + "/" + userId + "/" + filename;
        UUID currentUserId = getOptionalCurrentUserId();
        StoredFile metadata = fileService.getFileMetadataByStorageKey(storageKey, currentUserId);
        Resource resource = fileService.downloadFileByStorageKey(storageKey, currentUserId);
        return buildFileResponse(metadata, resource);
    }

    @GetMapping("/{fileId:[0-9a-fA-F\\-]{36}}/info")
    @Operation(summary = "Get file metadata")
    public FileResponse getFileInfo(@PathVariable UUID fileId) {
        UUID currentUserId = getOptionalCurrentUserId();
        return fileService.getFileInfo(fileId, currentUserId);
    }

    @DeleteMapping("/{fileId:[0-9a-fA-F\\-]{36}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete an uploaded file")
    public void deleteFile(@PathVariable UUID fileId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        fileService.deleteFile(fileId, currentUserId);
    }

    private ResponseEntity<Resource> buildFileResponse(StoredFile metadata, Resource resource) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(metadata.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        boolean isImage = metadata.getContentType() != null && metadata.getContentType().toLowerCase().startsWith("image/");

        ContentDisposition disposition;
        if (!isImage && (metadata.getFileType() == FileType.PROVIDER_DOCUMENT || metadata.getFileType() == FileType.OTHER)) {
            disposition = ContentDisposition.attachment()
                    .filename(metadata.getOriginalFilename())
                    .build();
        } else {
            disposition = ContentDisposition.inline()
                    .filename(metadata.getOriginalFilename())
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(metadata.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, isImage ? "public, max-age=86400" : "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private UUID getOptionalCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
