package com.kh.serviceplatform.common.email;

import com.kh.serviceplatform.common.email.dto.SmtpDiagnosticResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class SmtpDiagnosticService {

    private final boolean emailEnabled;
    private final String mailHost;
    private final int mailPort;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailFrom;

    public SmtpDiagnosticService(
            @Value("${app.mail.enabled:true}") boolean emailEnabled,
            @Value("${spring.mail.host:smtp.gmail.com}") String mailHost,
            @Value("${spring.mail.port:587}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom
    ) {
        this.emailEnabled = emailEnabled;
        this.mailHost = (mailHost != null && !mailHost.isBlank()) ? mailHost.trim() : "smtp.gmail.com";
        this.mailPort = mailPort > 0 ? mailPort : 587;
        this.mailUsername = mailUsername != null ? mailUsername.trim() : "";
        this.mailPassword = mailPassword != null ? mailPassword.trim() : "";
        this.mailFrom = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : this.mailUsername;
    }

    public SmtpDiagnosticResponse testConnectivity(String overrideHost, Integer overridePort) {
        String targetHost = (overrideHost != null && !overrideHost.isBlank()) ? overrideHost.trim() : mailHost;
        int targetPort = (overridePort != null && overridePort > 0) ? overridePort : mailPort;

        boolean dnsSuccess = false;
        long dnsElapsedMs = 0;
        List<String> resolvedIps = new ArrayList<>();
        String dnsError = null;

        // 1. DNS Resolution Test
        long dnsStart = System.currentTimeMillis();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(targetHost);
            dnsElapsedMs = System.currentTimeMillis() - dnsStart;
            dnsSuccess = true;
            resolvedIps = Arrays.stream(addresses)
                    .map(InetAddress::getHostAddress)
                    .toList();
        } catch (Exception ex) {
            dnsElapsedMs = System.currentTimeMillis() - dnsStart;
            dnsError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        // 2. TCP Handshake Test (5 second connect timeout)
        boolean tcpSuccess = false;
        long tcpElapsedMs = 0;
        String tcpErrorType = null;
        String tcpErrorMessage = null;

        if (dnsSuccess) {
            long tcpStart = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(targetHost, targetPort), 5000);
                tcpElapsedMs = System.currentTimeMillis() - tcpStart;
                tcpSuccess = true;
            } catch (Exception ex) {
                tcpElapsedMs = System.currentTimeMillis() - tcpStart;
                tcpErrorType = ex.getClass().getSimpleName();
                tcpErrorMessage = ex.getMessage();
            }
        }

        // 3. Formulate diagnosis summary
        String diagnosis;
        if (!dnsSuccess) {
            diagnosis = "DNS FAILURE: The container cannot resolve the host '" + targetHost + "'. Check container DNS settings.";
        } else if (!tcpSuccess) {
            diagnosis = "TCP/NETWORK FAILURE: DNS resolved " + targetHost + " to " + resolvedIps +
                    ", but TCP connection to port " + targetPort + " failed (" + tcpErrorType + ": " + tcpErrorMessage + "). " +
                    "Outbound traffic to port " + targetPort + " is blocked or dropped by the hosting network/firewall.";
        } else {
            diagnosis = "TCP SUCCESS: TCP connection to " + targetHost + ":" + targetPort + " established successfully in " + tcpElapsedMs + "ms. Network route is open.";
        }

        log.info("SMTP Diagnostic for {}:{}: DNS={}, TCP={} ({}ms)",
                targetHost, targetPort, dnsSuccess ? "OK" : "FAIL", tcpSuccess ? "OK" : "FAIL", tcpElapsedMs);

        return new SmtpDiagnosticResponse(
                emailEnabled,
                targetHost,
                targetPort,
                mailUsername,
                mailFrom,
                !mailPassword.isBlank(),
                true,
                true,
                dnsSuccess,
                dnsElapsedMs,
                resolvedIps,
                dnsError,
                tcpSuccess,
                tcpElapsedMs,
                tcpErrorType,
                tcpErrorMessage,
                diagnosis
        );
    }
}
