package com.kh.serviceplatform.common.exception;

import lombok.Getter;

@Getter
public class ChatRadiusExceededException extends BadRequestException {

    private final Double distanceKm;
    private final Double allowedRadiusKm;

    public ChatRadiusExceededException(Double distanceKm, Double allowedRadiusKm) {
        super("This provider is outside the allowed chat distance.");
        this.distanceKm = distanceKm;
        this.allowedRadiusKm = allowedRadiusKm;
    }
}
