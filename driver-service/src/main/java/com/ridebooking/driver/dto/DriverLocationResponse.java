package com.ridebooking.driver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Confirmation payload for a location update
 * ({@code PUT /api/v1/drivers/{driverId}/location}).
 */
@Data
@Builder
public class DriverLocationResponse {

    private Long driverId;

    /** Stored latitude, in range [−90, 90]. */
    private Double latitude;

    /** Stored longitude, in range [−180, 180]. */
    private Double longitude;

    /** When the location (and its freshness marker) was (re)written. */
    private Instant lastUpdated;
}
