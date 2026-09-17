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

    @PutMapping("/{driverId}/location")
    public ResponseEntity<DriverLocationResponse> updateLocation(
            @PathVariable Long driverId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody UpdateDriverLocationRequest request) {

        if (userId == null || userId.isBlank()) {
            throw new MissingIdentityException("X-User-Id header is required");
        }

        if (!"DRIVER".equalsIgnoreCase(userRole) && !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new OwnershipViolationException("Only drivers can update location");
        }

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            try {
                Long authId = Long.parseLong(userId);
                if (!authId.equals(driverId)) {
                    throw new OwnershipViolationException(
                            "Driver " + driverId + " location cannot be updated by user " + userId);
                }
            } catch (NumberFormatException e) {
                throw new MissingIdentityException("Invalid user identity");
            }
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
