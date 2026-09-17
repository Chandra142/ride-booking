package com.ridebooking.driver.controller;

import com.ridebooking.driver.dto.CreateDriverRequest;
import com.ridebooking.driver.dto.DriverResponse;
import com.ridebooking.driver.dto.UpdateDriverRequest;
import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.exception.ForbiddenException;
import com.ridebooking.driver.exception.MissingIdentityException;
import com.ridebooking.driver.exception.OwnershipViolationException;
import com.ridebooking.driver.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public DriverResponse createDriver(@Valid @RequestBody CreateDriverRequest request) {
        return driverService.createDriver(request);
    }

    @GetMapping("/{id}")
    public DriverResponse getDriverById(@PathVariable Long id) {
        return driverService.getDriverById(id);
    }

    @GetMapping
    public List<DriverResponse> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @PutMapping("/{id}")
    public DriverResponse updateDriver(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody UpdateDriverRequest request) {

        enforceOwnership(id, userId, userRole);
        return driverService.updateDriver(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteDriver(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        enforceOwnership(id, userId, userRole);
        driverService.deleteDriver(id);
    }

    @PatchMapping("/{id}/availability")
    public DriverResponse updateAvailability(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam AvailabilityStatus status) {

        enforceOwnership(id, userId, userRole);
        return driverService.updateAvailability(id, status);
    }

    @PostMapping("/{id}/release")
    public DriverResponse releaseDriver(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        enforceOwnership(id, userId, userRole);
        return driverService.releaseDriver(id);
    }

    private void enforceOwnership(Long driverId, String userId, String userRole) {
        if (userId == null || userId.isBlank()) {
            throw new MissingIdentityException("X-User-Id header is required");
        }

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return;
        }

        if (!"DRIVER".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("Only drivers can perform this action");
        }

        try {
            Long authId = Long.parseLong(userId);
            if (!authId.equals(driverId)) {
                throw new OwnershipViolationException(
                        "Driver " + driverId + " is not owned by user " + userId);
            }
        } catch (NumberFormatException e) {
            throw new MissingIdentityException("Invalid user identity");
        }
    }
}
