package com.ridebooking.driver.dto;

import lombok.Builder;
import lombok.Data;

/**
 * A single driver found by a geospatial "nearby" search.
 *
 * <p><b>Distance unit.</b> This is the only unit that crosses the network
 * boundary for geospatial results — <em>kilometers</em>. Redis stores and
 * returns metres internally; conversion (m → km) happens exactly once inside
 * {@link com.ridebooking.driver.service.redis.RedisDriverLocationService} so
 * no caller ever sees raw metres.</p>
 */
@Data
@Builder
public class NearbyDriverResponse {

    private Long driverId;

    /** Straight-line (geodesic) distance from the search origin, in kilometers. */
    private Double distanceKm;

    /** The driver's current latitude (geodesic search anchor). */
    private Double latitude;

    /** The driver's current longitude. */
    private Double longitude;
}
