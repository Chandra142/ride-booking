package com.ridebooking.ride.controller;

import com.ridebooking.ride.dto.RideRequestDto;
import com.ridebooking.ride.dto.RideResponseDto;
import com.ridebooking.ride.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping("/request")
    public ResponseEntity<RideResponseDto> requestRide(
            @Valid @RequestBody RideRequestDto request,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(rideService.requestRide(request, userId));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<RideResponseDto> acceptRide(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(rideService.acceptRide(id, userId, userRole));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<RideResponseDto> startRide(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(rideService.startRide(id, userId, userRole));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RideResponseDto> completeRide(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(rideService.completeRide(id, userId, userRole));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<RideResponseDto> cancelRide(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(rideService.cancelRide(id, userId, userRole));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponseDto> getRide(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(rideService.getRide(id, userId, userRole));
    }

    @GetMapping("/rider/{riderId}")
    public ResponseEntity<List<RideResponseDto>> getRidesByRider(
            @PathVariable Long riderId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(rideService.getRidesByRider(riderId, userId));
    }
}
