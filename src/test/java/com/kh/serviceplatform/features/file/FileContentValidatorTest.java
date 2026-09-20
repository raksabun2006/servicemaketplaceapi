package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.features.file.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileContentValidatorTest {

    private FileContentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileContentValidator();
    }

    @Test
    void shouldValidateValidPngImage() throws IOException {
        byte[] pngData = createSamplePngBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", pngData);

        String contentType = validator.validateAndDetectContentType(file, FileType.AVATAR);
        assertEquals("image/png", contentType);
    }

    @Test
    void shouldValidateValidPdfForProviderDocument() {
        byte[] pdfHeader = "%PDF-1.4 test document content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", pdfHeader);

        String contentType = validator.validateAndDetectContentType(file, FileType.PROVIDER_DOCUMENT);
        assertEquals("application/pdf", contentType);
    }

    @Test
    void shouldRejectExecutableFile() {
        byte[] dummyData = "MZ executable binary data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-dosexec", dummyData);

        assertThrows(BadRequestException.class, () ->
                validator.validateAndDetectContentType(file, FileType.AVATAR)
        );
    }

    @Test
    void shouldRejectMismatchedExtensionAndContent() {
        byte[] dummyData = "plain text pretending to be png".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", dummyData);

        assertThrows(BadRequestException.class, () ->
                validator.validateAndDetectContentType(file, FileType.AVATAR)
        );
    }

    @Test
    void shouldSanitizeDangerousPathTraversalFilename() {
        String sanitized = FileContentValidator.sanitizeFilename("../../etc/passwd");
        assertEquals("passwd", sanitized);

        String windowsSanitized = FileContentValidator.sanitizeFilename("..\\..\\secret.txt");
        assertEquals("secret.txt", windowsSanitized);
    }

    private byte[] createSamplePngBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
