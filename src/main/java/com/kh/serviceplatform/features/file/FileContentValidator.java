package com.kh.serviceplatform.features.file;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.features.file.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class FileContentValidator {

    private static final List<String> DISALLOWED_EXTENSIONS = List.of(
            "exe", "bat", "cmd", "sh", "jar", "class", "dll", "jsp", "php", "py", "js", "html", "svg", "hta", "vbs"
    );

    public String validateAndDetectContentType(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or not provided");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Filename cannot be empty");
        }

        String sanitizedName = sanitizeFilename(originalFilename);
        String extension = getExtension(sanitizedName).toLowerCase(Locale.ROOT);

        if (extension.isEmpty() || DISALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Disallowed or missing file extension: ." + extension);
        }

        byte[] header = readHeaderBytes(file, 16);
        String detectedMime = detectMimeType(header, extension);

        if (detectedMime == null) {
            throw new BadRequestException("File content does not match a supported valid format for extension ." + extension);
        }

        validateAllowedTypeForCategory(fileType, extension, detectedMime);

        // If it's a decodable image (JPEG/PNG), verify decoding and limit dimensions
        if (detectedMime.equals("image/jpeg") || detectedMime.equals("image/png")) {
            validateImageIntegrity(file);
        }

        return detectedMime;
    }

    private void validateAllowedTypeForCategory(FileType fileType, String extension, String mime) {
        switch (fileType) {
            case AVATAR, CATEGORY_ICON -> {
                if (!isImageMime(mime) || !isImageExtension(extension)) {
                    throw new BadRequestException("Avatars and Category Icons must be JPEG, PNG, or WebP images");
                }
            }
            case SERVICE_IMAGE, REQUEST_IMAGE, PORTFOLIO_IMAGE -> {
                if (!isImageMime(mime) || !isImageExtension(extension)) {
                    throw new BadRequestException("Image uploads must be JPEG, PNG, or WebP images");
                }
            }
            case PROVIDER_DOCUMENT -> {
                if (!mime.equals("application/pdf") && !isImageMime(mime)) {
                    throw new BadRequestException("Provider documents must be PDF, JPEG, or PNG");
                }
                if (!extension.equals("pdf") && !isImageExtension(extension)) {
                    throw new BadRequestException("Provider documents must have .pdf, .jpg, .jpeg, or .png extension");
                }
            }
            case OTHER -> {
                if (!mime.equals("application/pdf") && !isImageMime(mime) && !mime.equals("text/plain")) {
                    throw new BadRequestException("Unsupported file format");
                }
            }
        }
    }

    private boolean isImageMime(String mime) {
        return "image/jpeg".equals(mime) || "image/png".equals(mime) || "image/webp".equals(mime);
    }

    private boolean isImageExtension(String ext) {
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp");
    }

    private byte[] readHeaderBytes(MultipartFile file, int length) {
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            byte[] buffer = new byte[length];
            int read = is.read(buffer, 0, length);
            if (read < 4) {
                throw new BadRequestException("File is too small to be valid");
            }
            return buffer;
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    private String detectMimeType(byte[] header, String extension) {
        // JPEG: FF D8 FF
        if (header.length >= 3 &&
                (header[0] & 0xFF) == 0xFF &&
                (header[1] & 0xFF) == 0xD8 &&
                (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header.length >= 8 &&
                (header[0] & 0xFF) == 0x89 &&
                (header[1] & 0xFF) == 0x50 &&
                (header[2] & 0xFF) == 0x4E &&
                (header[3] & 0xFF) == 0x47 &&
                (header[4] & 0xFF) == 0x0D &&
                (header[5] & 0xFF) == 0x0A &&
                (header[6] & 0xFF) == 0x1A &&
                (header[7] & 0xFF) == 0x0A) {
            return "image/png";
        }

        // PDF: %PDF- (25 50 44 46 2D)
        if (header.length >= 5 &&
                header[0] == 0x25 &&
                header[1] == 0x50 &&
                header[2] == 0x44 &&
                header[3] == 0x46 &&
                header[4] == 0x2D) {
            return "application/pdf";
        }

        // WEBP: RIFF ... WEBP
        if (header.length >= 12 &&
                header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }

        // Plain text fallback if allowed extension
        if (extension.equals("txt") || extension.equals("csv")) {
            return "text/plain";
        }

        return null;
    }

    private void validateImageIntegrity(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new BadRequestException("Corrupted or unreadable image file");
            }
            if (image.getWidth() > 8192 || image.getHeight() > 8192) {
                throw new BadRequestException("Image dimensions exceed allowed limit of 8192x8192");
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to decode image: " + e.getMessage());
        }
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        String name = filename.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        name = name.replaceAll("[\\r\\n\\u0000]", "").trim();
        if (name.length() > 200) {
            String ext = getExtension(name);
            name = name.substring(0, 190) + "." + ext;
        }
        return name;
    }

    public static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
