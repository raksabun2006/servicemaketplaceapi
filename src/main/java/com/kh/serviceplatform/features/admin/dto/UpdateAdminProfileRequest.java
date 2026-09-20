package com.kh.serviceplatform.features.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "Request payload for updating admin profile")
public record UpdateAdminProfileRequest(
        @Size(max = 150)
        @Schema(description = "Full name of the admin", example = "Admin John")
        String fullName,

        @Size(max = 20)
        @Schema(description = "Contact phone number", example = "+85512345678")
        String phone,

        @Schema(description = "Avatar image file upload")
        MultipartFile avatarUploadFile,

        @Size(max = 100)
        @Schema(description = "Department name", example = "Operations")
        String department,

        @Size(max = 100)
        @Schema(description = "Position title", example = "System Administrator")
        String position,

        @Size(max = 50)
        @Schema(description = "Emergency contact info", example = "+85598765432")
        String emergencyContact
) {
}
