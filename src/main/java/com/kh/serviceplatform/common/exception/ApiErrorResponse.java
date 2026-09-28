package com.kh.serviceplatform.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors,
        Double distanceKm,
        Double allowedRadiusKm
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null, null, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, validationErrors, null, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path, Double distanceKm, Double allowedRadiusKm) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null, distanceKm, allowedRadiusKm);
    }
}
