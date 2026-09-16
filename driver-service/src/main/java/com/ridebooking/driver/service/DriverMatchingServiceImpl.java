package com.ridebooking.driver.service;

import com.ridebooking.driver.config.DriverLocationProperties;
import com.ridebooking.driver.dto.DriverMatchResponse;
import com.ridebooking.driver.dto.NearbyDriverResponse;
import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverMatchingServiceImpl implements DriverMatchingService {

    private final DriverLocationService locationService;
    private final DriverRepository driverRepository;
    private final DriverLocationProperties properties;
    private final StringRedisTemplate redis;

    @Override
    public DriverMatchResponse findAndAssignNearestDriver(
            double pickupLatitude, double pickupLongitude, double radiusKm) {

        double searchRadius = resolveRadius(radiusKm);

        log.info("Matching drivers near ({}, {}) within {} km",
                pickupLatitude, pickupLongitude, searchRadius);

        List<NearbyDriverResponse> nearbyDrivers =
                locationService.findNearbyDrivers(pickupLatitude, pickupLongitude, searchRadius);

        log.info("Found {} nearby candidates in Redis GEO", nearbyDrivers.size());

        for (NearbyDriverResponse candidate : nearbyDrivers) {
            Long driverId = candidate.getDriverId();

            if (!isLocationFresh(driverId)) {
                log.debug("Driver {} has stale location, skipping", driverId);
                continue;
            }

            int updated = driverRepository.updateAvailabilityStatus(
                    driverId, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY);

            if (updated == 1) {
                log.info("Assigned driver {} (distance: {} km)", driverId, candidate.getDistanceKm());
                return DriverMatchResponse.builder()
                        .driverId(driverId)
                        .distanceKm(candidate.getDistanceKm())
                        .build();
            }

            log.debug("Driver {} not ONLINE for assignment, trying next", driverId);
        }

        log.info("No eligible driver found near ({}, {})", pickupLatitude, pickupLongitude);
        return null;
    }

    private double resolveRadius(double requestedRadiusKm) {
        if (requestedRadiusKm <= 0) {
            return properties.getDefaultRadiusKm();
        }
        if (requestedRadiusKm > properties.getMaxRadiusKm()) {
            return properties.getMaxRadiusKm();
        }
        return requestedRadiusKm;
    }

    private boolean isLocationFresh(Long driverId) {
        String lastSeenKey = properties.getLastSeenKeyPrefix() + driverId;
        return Boolean.TRUE.equals(redis.hasKey(lastSeenKey));
    }
}
