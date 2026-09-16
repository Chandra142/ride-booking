package com.ridebooking.driver.validation;

import com.ridebooking.driver.exception.InvalidCoordinatesException;

/**
 * Validates geographic coordinates.
 *
 * <p>Rules (no silent clamping — invalid input is rejected):</p>
 * <ul>
 *   <li>latitude must be in [−90, 90]</li>
 *   <li>longitude must be in [−180, 180]</li>
 *   <li>null, {@code NaN}, {@link Double#POSITIVE_INFINITY} and
 *       {@link Double#NEGATIVE_INFINITY} are rejected</li>
 * </ul>
 *
 * <p>Rejects out-of-range values rather than clamping them, so a bad GPS
 * reading surfaces as a 400 instead of silently placing a driver at 90/180.</p>
 */
public final class CoordinateValidator {

    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;
    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;

    private CoordinateValidator() {
    }

    /**
     * Validates a coordinate pair. Throws {@link InvalidCoordinatesException}
     * on the first failing rule.
     */
    public static void validate(Double latitude, Double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    public static void validateLatitude(Double latitude) {
        if (latitude == null) {
            throw new InvalidCoordinatesException("latitude must not be null");
        }
        if (Double.isNaN(latitude)) {
            throw new InvalidCoordinatesException("latitude must not be NaN");
        }
        if (Double.isInfinite(latitude)) {
            throw new InvalidCoordinatesException("latitude must be finite");
        }
        if (latitude < LAT_MIN || latitude > LAT_MAX) {
            throw new InvalidCoordinatesException(
                    "latitude must be between -90.0 and 90.0 but was " + latitude);
        }
    }

    public static void validateLongitude(Double longitude) {
        if (longitude == null) {
            throw new InvalidCoordinatesException("longitude must not be null");
        }
        if (Double.isNaN(longitude)) {
            throw new InvalidCoordinatesException("longitude must not be NaN");
        }
        if (Double.isInfinite(longitude)) {
            throw new InvalidCoordinatesException("longitude must be finite");
        }
        if (longitude < LON_MIN || longitude > LON_MAX) {
            throw new InvalidCoordinatesException(
                    "longitude must be between -180.0 and 180.0 but was " + longitude);
        }
    }

    /** Upper bound used by the validator for latitude. */
    public static double latMax() {
        return LAT_MAX;
    }
}
