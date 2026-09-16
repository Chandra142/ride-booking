package com.ridebooking.driver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.ridebooking.driver.dto.DriverLocationResponse;
import com.ridebooking.driver.dto.DriverResponse;
import com.ridebooking.driver.dto.NearbyDriverResponse;
import com.ridebooking.driver.dto.UpdateDriverLocationRequest;
import com.ridebooking.driver.service.DriverService;
import com.ridebooking.driver.service.DriverLocationService;
import com.ridebooking.driver.validation.CoordinateValidator;
import com.ridebooking.driver.exception.MissingIdentityException;
import com.ridebooking.driver.exception.OwnershipViolationException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverLocationController {

    private final DriverService driverService;
    private final DriverLocationService locationService;

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverLocationResponse> updateLocation(
            @PathVariable Long driverId,
            @RequestHeader(value = "X-User-Id", required = false) String identity,
            @Valid @RequestBody UpdateDriverLocationRequest request) {

        if (identity == null || identity.isBlank()) {
            throw new MissingIdentityException("X-User-Id is required");
        }

        DriverResponse owner = driverService.getDriverById(driverId);
        if (!identity.equalsIgnoreCase(owner.getEmail())) {
            throw new OwnershipViolationException(
                    "Driver " + driverId + " is not owned by " + identity);
        }

        CoordinateValidator.validate(
                request.getLatitude(), request.getLongitude());

        locationService.updateLocation(
                driverId,
                request.getLatitude(),
                request.getLongitude());

        return ResponseEntity.ok(DriverLocationResponse.builder()
                .driverId(driverId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .lastUpdated(Instant.now())
                .build());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyDriverResponse>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double radiusKm) {
        List<NearbyDriverResponse> nearby = locationService.findNearbyDrivers(
                latitude, longitude, radiusKm);
        return ResponseEntity.ok(nearby);
    }
}
