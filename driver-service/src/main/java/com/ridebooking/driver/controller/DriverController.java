package com.ridebooking.driver.controller;

import com.ridebooking.driver.dto.CreateDriverRequest;
import com.ridebooking.driver.dto.DriverResponse;
import com.ridebooking.driver.dto.UpdateDriverRequest;
import com.ridebooking.driver.entity.AvailabilityStatus;
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
            @Valid @RequestBody UpdateDriverRequest request) {
        return driverService.updateDriver(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
    }

    @PatchMapping("/{id}/availability")
    public DriverResponse updateAvailability(
            @PathVariable Long id,
            @RequestParam AvailabilityStatus status) {
        return driverService.updateAvailability(id, status);
    }

    @PostMapping("/{id}/release")
    public DriverResponse releaseDriver(@PathVariable Long id) {
        return driverService.releaseDriver(id);
    }
}