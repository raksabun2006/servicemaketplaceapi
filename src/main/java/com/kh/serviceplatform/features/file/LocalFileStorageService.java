package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.file.storage.base-path:./uploads}") String basePath) {
        this.rootLocation = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize file storage directory: {}", rootLocation, e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String upload(MultipartFile file, String directory, String filename) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        try {
            Path targetDir = rootLocation.resolve(directory).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new BadRequestException("Invalid storage directory path");
            }

            Files.createDirectories(targetDir);

            Path destinationFile = targetDir.resolve(filename).normalize();
            if (!destinationFile.startsWith(targetDir)) {
                throw new BadRequestException("Path traversal attempt detected in filename");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String storageKey = directory + "/" + filename;
            log.debug("File stored successfully: {}", storageKey);
            return storageKey;

        } catch (IOException e) {
            log.error("Failed to store file in directory: {}", directory, e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource download(String storageKey) {
        try {
            Path file = resolveAndValidate(storageKey);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or not readable: " + storageKey);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error resolving file URL: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path file = resolveAndValidate(storageKey);
            Files.deleteIfExists(file);
            log.debug("File deleted from storage: {}", storageKey);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storageKey, e);
            throw new RuntimeException("Failed to delete stored file", e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path file = resolveAndValidate(storageKey);
            return Files.exists(file) && Files.isReadable(file);
        } catch (Exception e) {
            return false;
        }
    }

    private Path resolveAndValidate(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new BadRequestException("Storage key cannot be empty");
        }

        Path resolved = rootLocation.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new BadRequestException("Path traversal attempt detected in storage key");
        }

        return resolved;
    }
}
