package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString());
        storageService.init();
    }

    @Test
    void shouldUploadAndDownloadFileSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image bytes".getBytes()
        );

        String storageKey = storageService.upload(file, "avatars/user-123", "random-id.jpg");
        assertNotNull(storageKey);
        assertTrue(storageService.exists(storageKey));

        Resource resource = storageService.download(storageKey);
        assertTrue(resource.exists());

        try (InputStream is = resource.getInputStream()) {
            assertEquals("test image bytes", new String(is.readAllBytes()));
        }

        storageService.delete(storageKey);
        assertFalse(storageService.exists(storageKey));
    }

    @Test
    void shouldBlockPathTraversalInUploadDirectory() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test bytes".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                storageService.upload(file, "../../dangerous", "test.jpg")
        );
    }

    @Test
    void shouldBlockPathTraversalInFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test bytes".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                storageService.upload(file, "avatars", "../../test.jpg")
        );
    }
}
