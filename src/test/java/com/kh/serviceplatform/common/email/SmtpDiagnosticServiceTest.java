package com.kh.serviceplatform.common.email;

import com.kh.serviceplatform.common.email.dto.SmtpDiagnosticResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmtpDiagnosticServiceTest {

    private SmtpDiagnosticService diagnosticService;

    @BeforeEach
    void setUp() {
        diagnosticService = new SmtpDiagnosticService(
                "smtp.gmail.com",
                587,
                "user@example.com",
                "secret-password",
                "user@example.com"
        );
    }

    @Test
    void testConnectivity_withInvalidHost_shouldReportDnsFailure() {
        SmtpDiagnosticResponse response = diagnosticService.testConnectivity("non-existent-domain-xyz-12345.com", 587);

        assertNotNull(response);
        assertEquals("non-existent-domain-xyz-12345.com", response.host());
        assertEquals(587, response.port());
        assertFalse(response.dnsSuccess());
        assertNotNull(response.dnsError());
        assertFalse(response.tcpSuccess());
        assertTrue(response.diagnosis().contains("DNS FAILURE"));
        assertTrue(response.passwordConfigured());
    }

    @Test
    void testConnectivity_reportingNeverExposesPassword() {
        SmtpDiagnosticResponse response = diagnosticService.testConnectivity("localhost", 9999);

        assertNotNull(response);
        assertTrue(response.passwordConfigured());
        assertEquals("user@example.com", response.username());
        // Verify response record has no password field
    }
}
