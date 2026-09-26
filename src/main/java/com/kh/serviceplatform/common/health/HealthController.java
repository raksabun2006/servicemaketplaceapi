package com.kh.serviceplatform.common.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "System and container health monitoring")
public class HealthController {

    @Operation(summary = "Health probe for Railway and container orchestrators")
    @GetMapping({"/health", "/actuator/health"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "serviceplatform"
        ));
    }
}
