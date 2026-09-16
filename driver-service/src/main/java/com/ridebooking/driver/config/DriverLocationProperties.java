package com.ridebooking.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable knobs for the Redis-backed driver location index (Stage 3).
 *
 * <p><b>Why Redis, not PostgreSQL?</b> Live driver coordinates are
 * high-frequency, ephemeral data. Storing them in PostgreSQL would bloat the
 * transactional {@code drivers} table with columns that need constant writes
 * and would force the ride-service to scan the whole table for every nearby
 * search. Redis GEO keeps transient coordinates out of the durability-critical
 * row store and makes geospatial queries O(log N) over the index.</p>
 *
 * <p><b>Why per-driver freshness keys instead of a TTL on the whole GEO
 * set?</b> Redis allows a TTL on a whole key, but {@code drivers:locations}
 * is a single GEO set shared by every driver. Expiring it would evict every
 * driver at once. So the GEO set itself <b>never</b> carries a TTL; freshness
 * is tracked <b>per driver</b> with individual "last seen" marker keys that
 * carry their own TTL ({@code DRIVER_LOCATION_TTL_SECONDS}, default 300). A
 * driver is fresh while their marker key exists; a member without a live
 * marker is stale and is removed lazily by nearby searches and periodically
 * by a sweep.</p>
 *
 * <p><b>Redis GEO key design.</b></p>
 * <pre>
 *   drivers:locations                          (GEO set, NO TTL)
 *        member  driver:7                      (one member per driver id)
 *
 *   driver:location:last-seen:7  → timestamp   (TTL, per driver)
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "driver.location")
public class DriverLocationProperties {

    /** Redis GEO key holding all live driver locations. */
    private String geoKey = "drivers:locations";

    /** Prefix used to derive a driver's GEO member ("driver:7"). */
    private String memberKeyPrefix = "driver:";

    /** Per-driver last-seen freshness marker key prefix. */
    private String lastSeenKeyPrefix = "driver:location:last-seen:";

    /** Per-driver freshness marker TTL, seconds (default 300 = 5 min). */
    private long ttlSeconds = 300;

    /** Default nearby-search radius (km) when the caller omits it. */
    private double defaultRadiusKm = 5.0;

    /** Hard upper bound on the nearby-search radius (km). */
    private double maxRadiusKm = 50.0;

    /** When true, a periodic sweep removes stale GEO members. */
    private boolean cleanupEnabled = true;

    /** Fixed delay between stale-location sweeps (seconds). */
    private long sweepIntervalSeconds = 60;
}
