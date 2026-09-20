package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.file.dto.FileUploadResponse;
import com.kh.serviceplatform.features.file.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileContentValidator fileContentValidator;

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    private User customerUser;
    private User providerUser;
    private UUID customerId;
    private UUID providerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerUser = User.builder()
                .id(customerId)
                .email("customer@example.com")
                .role(UserRole.CUSTOMER)
                .fullName("Customer User")
                .build();

        providerId = UUID.randomUUID();
        providerUser = User.builder()
                .id(providerId)
                .email("provider@example.com")
                .role(UserRole.PROVIDER)
                .fullName("Provider User")
                .build();
    }

    @Test
    void shouldAllowCustomerToUploadAvatar() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[100]);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));
        when(fileContentValidator.validateAndDetectContentType(file, FileType.AVATAR)).thenReturn("image/png");
        when(fileStorageService.upload(eq(file), anyString(), anyString())).thenReturn("avatars/" + customerId + "/file.png");

        StoredFile storedFile = StoredFile.builder()
                .id(UUID.randomUUID())
                .owner(customerUser)
                .originalFilename("photo.png")
                .storedFilename("file.png")
                .storageKey("avatars/" + customerId + "/file.png")
                .contentType("image/png")
                .fileSize(100)
                .fileType(FileType.AVATAR)
                .build();

        when(fileRepository.save(any(StoredFile.class))).thenReturn(storedFile);
        when(fileMapper.toUploadResponse(storedFile)).thenReturn(
                new FileUploadResponse(storedFile.getId(), "photo.png", "image/png", 100, FileType.AVATAR, "/api/v1/files/" + storedFile.getId())
        );

        FileUploadResponse response = fileService.uploadFile(file, FileType.AVATAR, customerId);

        assertNotNull(response);
        assertEquals(FileType.AVATAR, response.fileType());
        verify(userRepository).save(customerUser); // Updated avatar
    }

    @Test
    void shouldDisallowCustomerFromUploadingServiceImage() {
        MockMultipartFile file = new MockMultipartFile("file", "service.png", "image/png", new byte[100]);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));

        assertThrows(ForbiddenException.class, () ->
                fileService.uploadFile(file, FileType.SERVICE_IMAGE, customerId)
        );

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void shouldAllowCustomerToUploadProviderDocumentForOnboarding() {
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", new byte[100]);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));
        when(fileContentValidator.validateAndDetectContentType(file, FileType.PROVIDER_DOCUMENT)).thenReturn("application/pdf");
        when(fileStorageService.upload(eq(file), anyString(), anyString())).thenReturn("provider-documents/" + customerId + "/doc.pdf");

        StoredFile storedFile = StoredFile.builder()
                .id(UUID.randomUUID())
                .owner(customerUser)
                .originalFilename("license.pdf")
                .storedFilename("doc.pdf")
                .storageKey("provider-documents/" + customerId + "/doc.pdf")
                .contentType("application/pdf")
                .fileSize(100)
                .fileType(FileType.PROVIDER_DOCUMENT)
                .build();

        when(fileRepository.save(any(StoredFile.class))).thenReturn(storedFile);
        when(fileMapper.toUploadResponse(storedFile)).thenReturn(
                new FileUploadResponse(storedFile.getId(), "license.pdf", "application/pdf", 100, FileType.PROVIDER_DOCUMENT, "/api/v1/files/" + storedFile.getId())
        );

        FileUploadResponse response = fileService.uploadFile(file, FileType.PROVIDER_DOCUMENT, customerId);
        assertNotNull(response);
        assertEquals(FileType.PROVIDER_DOCUMENT, response.fileType());
    }

    @Test
    void shouldAllowProviderToUploadProviderDocument() {
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", new byte[100]);

        when(userRepository.findById(providerId)).thenReturn(Optional.of(providerUser));
        when(fileContentValidator.validateAndDetectContentType(file, FileType.PROVIDER_DOCUMENT)).thenReturn("application/pdf");
        when(fileStorageService.upload(eq(file), anyString(), anyString())).thenReturn("provider-documents/" + providerId + "/doc.pdf");

        StoredFile storedFile = StoredFile.builder()
                .id(UUID.randomUUID())
                .owner(providerUser)
                .originalFilename("license.pdf")
                .storedFilename("doc.pdf")
                .storageKey("provider-documents/" + providerId + "/doc.pdf")
                .contentType("application/pdf")
                .fileSize(100)
                .fileType(FileType.PROVIDER_DOCUMENT)
                .build();

        when(fileRepository.save(any(StoredFile.class))).thenReturn(storedFile);
        when(fileMapper.toUploadResponse(storedFile)).thenReturn(
                new FileUploadResponse(storedFile.getId(), "license.pdf", "application/pdf", 100, FileType.PROVIDER_DOCUMENT, "/api/v1/files/" + storedFile.getId())
        );

        FileUploadResponse response = fileService.uploadFile(file, FileType.PROVIDER_DOCUMENT, providerId);
        assertNotNull(response);
        assertEquals(FileType.PROVIDER_DOCUMENT, response.fileType());
    }

    @Test
    void shouldRejectOversizedFile() {
        // 6 MB file for Avatar (limit is 5 MB)
        byte[] largeBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));

        assertThrows(BadRequestException.class, () ->
                fileService.uploadFile(file, FileType.AVATAR, customerId)
        );
    }

    @Test
    void shouldDownloadFileByStorageKey() {
        String storageKey = "request-images/" + customerId + "/image.png";
        StoredFile storedFile = StoredFile.builder()
                .id(UUID.randomUUID())
                .owner(customerUser)
                .originalFilename("image.png")
                .storedFilename("image.png")
                .storageKey(storageKey)
                .contentType("image/png")
                .fileSize(100)
                .fileType(FileType.REQUEST_IMAGE)
                .build();

        when(fileRepository.findByStorageKey(storageKey)).thenReturn(Optional.of(storedFile));
        when(fileStorageService.download(storageKey)).thenReturn(new ByteArrayResource(new byte[100]));

        Resource resource = fileService.downloadFileByStorageKey(storageKey, customerId);
        assertNotNull(resource);
    }
}
