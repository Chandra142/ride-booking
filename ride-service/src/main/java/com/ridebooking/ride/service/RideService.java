package com.ridebooking.ride.service;

import com.ridebooking.ride.client.DriverServiceClient;
import com.ridebooking.ride.dto.DriverMatchResult;
import com.ridebooking.ride.dto.RideRequestDto;
import com.ridebooking.ride.dto.RideResponseDto;
import com.ridebooking.ride.entity.Ride;
import com.ridebooking.ride.entity.RideStatus;
import com.ridebooking.ride.exception.ForbiddenException;
import com.ridebooking.ride.exception.NoDriverAvailableException;
import com.ridebooking.ride.exception.RideNotFoundException;
import com.ridebooking.ride.exception.InvalidRideStateException;
import com.ridebooking.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;

    @Value("${ride.matching.radius-km:10.0}")
    private double matchingRadiusKm;

    private static final double BASE_FARE = 30.0;
    private static final double RATE_PER_KM = 12.0;

    public RideResponseDto requestRide(RideRequestDto request, String userId) {
        Long authenticatedUserId = parseUserId(userId);

        if (!authenticatedUserId.equals(request.getRiderId())) {
            throw new ForbiddenException("You can only request rides for yourself");
        }

        log.info("Ride requested by rider {} from ({}, {})",
                request.getRiderId(), request.getPickupLatitude(), request.getPickupLongitude());

        Ride ride = Ride.builder()
                .riderId(request.getRiderId())
                .pickupLocation(request.getPickupLocation())
                .dropLocation(request.getDropLocation())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .dropLatitude(request.getDropLatitude())
                .dropLongitude(request.getDropLongitude())
                .status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        ride = rideRepository.save(ride);

        DriverMatchResult match = driverServiceClient.findAndAssignDriver(
                request.getPickupLatitude(), request.getPickupLongitude(), matchingRadiusKm)
                .orElse(null);

        if (match == null) {
            log.info("No driver available for ride {}", ride.getId());
            throw new NoDriverAvailableException();
        }

        ride.setDriverId(match.getDriverId());
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);
        ride = rideRepository.save(ride);

        log.info("Ride {} assigned to driver {} (distance: {} km)",
                ride.getId(), match.getDriverId(), match.getDistanceKm());

        return toDto(ride);
    }

    public RideResponseDto acceptRide(Long rideId, String userId, String userRole) {
        Ride ride = getRideOrThrow(rideId);
        requireDriverRole(userRole);
        requireStatus(ride, RideStatus.DRIVER_ASSIGNED, "Ride must be DRIVER_ASSIGNED to be accepted");
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        return toDto(rideRepository.save(ride));
    }

    public RideResponseDto startRide(Long rideId, String userId, String userRole) {
        Ride ride = getRideOrThrow(rideId);
        requireDriverRole(userRole);
        requireStatus(ride, RideStatus.ACCEPTED, "Ride must be ACCEPTED to start");
        ride.setStatus(RideStatus.ONGOING);
        ride.setStartedAt(LocalDateTime.now());
        return toDto(rideRepository.save(ride));
    }

    public RideResponseDto completeRide(Long rideId, String userId, String userRole) {
        Ride ride = getRideOrThrow(rideId);
        requireDriverRole(userRole);
        requireStatus(ride, RideStatus.ONGOING, "Ride must be ONGOING to complete");

        double distanceKm = calculateDistanceKm(
                ride.getPickupLatitude(), ride.getPickupLongitude(),
                ride.getDropLatitude(), ride.getDropLongitude());

        double fare = BASE_FARE + (distanceKm * RATE_PER_KM);

        ride.setFare(Math.round(fare * 100.0) / 100.0);
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride = rideRepository.save(ride);

        releaseDriverIfAssigned(ride);

        return toDto(ride);
    }

    public RideResponseDto cancelRide(Long rideId, String userId, String userRole) {
        Ride ride = getRideOrThrow(rideId);
        Long authenticatedUserId = parseUserId(userId);

        boolean isRider = authenticatedUserId.equals(ride.getRiderId());
        boolean isDriver = "DRIVER".equalsIgnoreCase(userRole);

        if (!isRider && !isDriver) {
            throw new ForbiddenException("Only the rider or assigned driver can cancel a ride");
        }

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException("Cannot cancel a ride that is already " + ride.getStatus());
        }
        ride.setStatus(RideStatus.CANCELLED);
        ride = rideRepository.save(ride);

        releaseDriverIfAssigned(ride);

        return toDto(ride);
    }

    public RideResponseDto getRide(Long rideId, String userId, String userRole) {
        Ride ride = getRideOrThrow(rideId);
        Long authenticatedUserId = parseUserId(userId);

        boolean isRider = authenticatedUserId.equals(ride.getRiderId());
        boolean isDriver = "DRIVER".equalsIgnoreCase(userRole);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

        if (!isRider && !isDriver && !isAdmin) {
            throw new ForbiddenException("You do not have access to this ride");
        }

        return toDto(ride);
    }

    public List<RideResponseDto> getRidesByRider(Long riderId, String userId) {
        Long authenticatedUserId = parseUserId(userId);

        if (!authenticatedUserId.equals(riderId)) {
            throw new ForbiddenException("You can only view your own rides");
        }

        return rideRepository.findByRiderId(riderId).stream().map(this::toDto).toList();
    }

    private void releaseDriverIfAssigned(Ride ride) {
        if (ride.getDriverId() != null) {
            driverServiceClient.releaseDriver(ride.getDriverId());
        }
    }

    private Ride getRideOrThrow(Long id) {
        return rideRepository.findById(id).orElseThrow(() -> new RideNotFoundException(id));
    }

    private void requireStatus(Ride ride, RideStatus expected, String message) {
        if (ride.getStatus() != expected) {
            throw new InvalidRideStateException(message + " (current status: " + ride.getStatus() + ")");
        }
    }

    private void requireDriverRole(String userRole) {
        if (!"DRIVER".equalsIgnoreCase(userRole) && !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("Only drivers can perform this action");
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid user identity");
        }
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private RideResponseDto toDto(Ride ride) {
        return RideResponseDto.builder()
                .id(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .pickupLocation(ride.getPickupLocation())
                .dropLocation(ride.getDropLocation())
                .status(ride.getStatus())
                .fare(ride.getFare())
                .requestedAt(ride.getRequestedAt())
                .completedAt(ride.getCompletedAt())
                .build();
    }
}
