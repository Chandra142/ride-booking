package com.ridebooking.ride.client;

import com.ridebooking.ride.dto.DriverDto;
import com.ridebooking.ride.dto.DriverMatchResult;
import com.ridebooking.ride.exception.DriverServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverServiceClient {

    private final RestTemplate restTemplate;

    public List<DriverDto> getAvailableDrivers() {
        try {
            log.info("Calling DRIVER-SERVICE to get available drivers...");

            DriverDto[] drivers = restTemplate.getForObject(
                    "http://DRIVER-SERVICE/api/v1/drivers",
                    DriverDto[].class
            );

            if (drivers == null) {
                log.info("No drivers returned from driver service");
                return List.of();
            }

            log.info("Found {} total drivers", drivers.length);

            List<DriverDto> onlineDrivers = Arrays.stream(drivers)
                    .filter(driver -> "ONLINE".equalsIgnoreCase(driver.getAvailabilityStatus()))
                    .toList();

            log.info("Found {} ONLINE drivers", onlineDrivers.size());
            return onlineDrivers;

        } catch (Exception e) {
            log.error("Error calling driver service: {}", e.getMessage());
            return List.of();
        }
    }

    public Optional<DriverMatchResult> findAndAssignDriver(
            double pickupLatitude, double pickupLongitude, double radiusKm) {
        try {
            Map<String, Object> request = Map.of(
                    "latitude", pickupLatitude,
                    "longitude", pickupLongitude,
                    "radiusKm", radiusKm
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<DriverMatchResult> response = restTemplate.exchange(
                    "http://DRIVER-SERVICE/api/v1/drivers/matching/assign",
                    HttpMethod.POST,
                    entity,
                    DriverMatchResult.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Assigned driver {} at {} km",
                        response.getBody().getDriverId(), response.getBody().getDistanceKm());
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            log.info("No eligible driver found near ({}, {})", pickupLatitude, pickupLongitude);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Driver matching service unavailable: {}", e.getMessage());
            throw new DriverServiceUnavailableException(
                    "Driver matching service is unavailable: " + e.getMessage());
        }
    }

    public void releaseDriver(Long driverId) {
        try {
            restTemplate.postForObject(
                    "http://DRIVER-SERVICE/api/v1/drivers/" + driverId + "/release",
                    null,
                    Void.class);
            log.info("Released driver {} back to ONLINE", driverId);
        } catch (Exception e) {
            log.warn("Failed to release driver {}: {}", driverId, e.getMessage());
        }
    }
}
