package com.ridebooking.driver.service;

import com.ridebooking.driver.dto.NearbyDriverResponse;

import java.util.List;

/**
 * Contract for the live driver-location index (Stage 3).
 *
 * <p>The index is backed by Redis GEO, not PostgreSQL. Live coordinates are
 * ephemeral, high-frequency data — storing them in the transactional
 * {@code drivers} table would bloat the row store with columns that need
 * constant writes and would force the ride-service to scan the whole table
 * for every nearby search. Redis GEO keeps transient coordinates out of
 * PostgreSQL and serves geospatial queries in O(log N) over the index.</p>
 *
 * <p><b>Key design (see {@code DriverLocationProperties}).</b></p>
 * <pre>
 *   drivers:locations                              (GEO set, NO TTL)
 *        member  driver:{driverId}                 (one member per driver)
 *
 *   driver:location:last-seen:{driverId}           (marker key, TTL)
 * </pre>
 * The shared GEO set never carries a TTL (expiring it would evict every
 * driver at once); freshness is instead tracked <b>per driver</b> with the
 * individual marker keys above, each of which owns its own TTL.
 */
public interface DriverLocationService {

    /**
     * Upserts a driver's live coordinates into the GEO index.
     *
     * @param driverId  the owning driver (must exist in PostgreSQL)
     * @param latitude  in range [-90, 90]
     * @param longitude in range [-180, 180]
     */
    void updateLocation(Long driverId, double latitude, double longitude);

    /**
     * Removes a driver from the GEO index (used on OFFLINE and on delete).
     *
     * @param driverId the driver whose member should be dropped
     */
    void removeLocation(Long driverId);

    /**
     * Returns drivers whose coordinates are within {@code radiusKm} of the
     * search point, ordered by ascending distance, with each hit carrying its
     * geodesic distance in kilometres.
     *
     * @param latitude  search-centre latitude
     * @param longitude search-centre longitude
     * @param radiusKm  search radius in kilometres (bounded by the caller)
     * @return matching {@link NearbyDriverResponse}s (never null, possibly empty)
     */
    List<NearbyDriverResponse> findNearbyDrivers(double latitude, double longitude, double radiusKm);
}
