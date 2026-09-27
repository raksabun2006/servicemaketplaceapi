package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.common.email.SmtpDiagnosticService;
import com.kh.serviceplatform.common.email.dto.SmtpDiagnosticResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Diagnostics", description = "Admin-only system and network diagnostic operations")
@RestController
@RequestMapping("/api/v1/admin/diagnostics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDiagnosticsController {

    private final SmtpDiagnosticService smtpDiagnosticService;

    @GetMapping("/smtp")
    @Operation(summary = "Test SMTP connectivity", description = "Tests DNS resolution and TCP handshake to SMTP server without sending email. Accessible only to ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diagnostic check completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public SmtpDiagnosticResponse testSmtp(
            @RequestParam(required = false) String host,
            @RequestParam(required = false) Integer port
    ) {
        return smtpDiagnosticService.testConnectivity(host, port);
    }
}
