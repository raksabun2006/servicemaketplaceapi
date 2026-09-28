package com.kh.serviceplatform.common.util;

import com.kh.serviceplatform.common.exception.BadRequestException;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("Latitude and longitude must not be null");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new BadRequestException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Longitude must be between -180 and 180 degrees");
        }
    }

    public static void validateRadius(Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0.0 || radiusKm > 500.0) {
            throw new BadRequestException("Radius must be greater than 0 and at most 500 km");
        }
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceKm(lat1, lon1, lat2, lon2, 1);
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2, int decimalPlaces) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0) * Math.cos(rLat1) * Math.cos(rLat2);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        double distance = EARTH_RADIUS_KM * c;
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(distance * factor) / factor;
    }
}
