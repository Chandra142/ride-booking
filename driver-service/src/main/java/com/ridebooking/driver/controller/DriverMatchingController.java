package com.ridebooking.driver.controller;

import com.ridebooking.driver.dto.DriverMatchResponse;
import com.ridebooking.driver.dto.NearbyMatchRequest;
import com.ridebooking.driver.service.DriverMatchingService;
import com.ridebooking.driver.validation.CoordinateValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers/matching")
@RequiredArgsConstructor
public class DriverMatchingController {

    private final DriverMatchingService matchingService;

    @PostMapping("/assign")
    public ResponseEntity<DriverMatchResponse> assignDriver(
            @Valid @RequestBody NearbyMatchRequest request) {

        CoordinateValidator.validate(request.getLatitude(), request.getLongitude());

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(
                request.getLatitude(), request.getLongitude(), request.getRadiusKm());

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }
}
