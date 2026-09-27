package com.kh.serviceplatform.common.email.dto;

import java.util.List;

public record SmtpDiagnosticResponse(
        boolean emailEnabled,
        String host,
        int port,
        String username,
        String mailFrom,
        boolean passwordConfigured,
        boolean starttlsEnabled,
        boolean authEnabled,
        boolean dnsSuccess,
        long dnsElapsedMs,
        List<String> resolvedIps,
        String dnsError,
        boolean tcpSuccess,
        long tcpElapsedMs,
        String tcpErrorType,
        String tcpErrorMessage,
        String diagnosis
) {}
