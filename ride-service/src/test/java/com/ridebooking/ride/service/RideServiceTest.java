package com.ridebooking.ride.service;

import com.ridebooking.ride.client.DriverServiceClient;
import com.ridebooking.ride.dto.DriverMatchResult;
import com.ridebooking.ride.dto.RideRequestDto;
import com.ridebooking.ride.dto.RideResponseDto;
import com.ridebooking.ride.entity.Ride;
import com.ridebooking.ride.entity.RideStatus;
import com.ridebooking.ride.exception.DriverServiceUnavailableException;
import com.ridebooking.ride.exception.InvalidRideStateException;
import com.ridebooking.ride.exception.NoDriverAvailableException;
import com.ridebooking.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverServiceClient driverServiceClient;

    @InjectMocks
    private RideService rideService;

    private RideRequestDto baseRequest;

    @BeforeEach
    void setUp() throws Exception {
        baseRequest = new RideRequestDto();
        baseRequest.setRiderId(1L);
        baseRequest.setPickupLocation("MG Road");
        baseRequest.setDropLocation("Whitefield");
        baseRequest.setPickupLatitude(12.9758);
        baseRequest.setPickupLongitude(77.6045);
        baseRequest.setDropLatitude(12.9698);
        baseRequest.setDropLongitude(77.7500);

        java.lang.reflect.Field field = RideService.class.getDeclaredField("matchingRadiusKm");
        field.setAccessible(true);
        field.setDouble(rideService, 10.0);
    }

    private void mockSaveReturnId() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(1L);
            return ride;
        });
    }

    @Test
    void requestRide_shouldAssignNearestDriver() {
        mockSaveReturnId();

        when(driverServiceClient.findAndAssignDriver(12.9758, 77.6045, 10.0))
                .thenReturn(Optional.of(new DriverMatchResult(101L, 0.5)));

        RideResponseDto response = rideService.requestRide(baseRequest);

        assertThat(response.getDriverId()).isEqualTo(101L);
        assertThat(response.getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(response.getRiderId()).isEqualTo(1L);
    }

    @Test
    void requestRide_shouldThrowException_whenNoDriversAvailable() {
        mockSaveReturnId();

        when(driverServiceClient.findAndAssignDriver(12.9758, 77.6045, 10.0))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.requestRide(baseRequest))
                .isInstanceOf(NoDriverAvailableException.class)
                .hasMessage("No drivers currently available");
    }

    @Test
    void requestRide_shouldThrow_whenDriverServiceUnavailable() {
        mockSaveReturnId();

        when(driverServiceClient.findAndAssignDriver(anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new DriverServiceUnavailableException("Redis connection refused"));

        assertThatThrownBy(() -> rideService.requestRide(baseRequest))
                .isInstanceOf(DriverServiceUnavailableException.class)
                .hasMessageContaining("Redis connection refused");
    }

    @Test
    void requestRide_shouldFallBackToNextDriver_whenNearestUnavailable() {
        mockSaveReturnId();

        when(driverServiceClient.findAndAssignDriver(12.9758, 77.6045, 10.0))
                .thenReturn(Optional.of(new DriverMatchResult(202L, 1.2)));

        RideResponseDto response = rideService.requestRide(baseRequest);

        assertThat(response.getDriverId()).isEqualTo(202L);
        assertThat(response.getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
    }

    @Test
    void acceptRide_shouldThrowException_whenRideNotInDriverAssignedState() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));

        assertThatThrownBy(() -> rideService.acceptRide(5L))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("Ride must be DRIVER_ASSIGNED to be accepted");
    }

    @Test
    void acceptRide_shouldTransitionToAccepted() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .status(RideStatus.DRIVER_ASSIGNED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

        RideResponseDto response = rideService.acceptRide(5L);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
    }

    @Test
    void startRide_shouldTransitionToOngoing() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

        RideResponseDto response = rideService.startRide(5L);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ONGOING);
    }

    @Test
    void startRide_shouldThrow_whenNotAccepted() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .status(RideStatus.DRIVER_ASSIGNED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));

        assertThatThrownBy(() -> rideService.startRide(5L))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("Ride must be ACCEPTED to start");
    }

    @Test
    void completeRide_shouldTransitionToCompleted_andReleaseDriver() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .pickupLatitude(12.9758)
                .pickupLongitude(77.6045)
                .dropLatitude(12.9698)
                .dropLongitude(77.7500)
                .status(RideStatus.ONGOING)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

        RideResponseDto response = rideService.completeRide(5L);

        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(response.getFare()).isNotNull();
        verify(driverServiceClient).releaseDriver(101L);
    }

    @Test
    void completeRide_shouldThrow_whenNotOngoing() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));

        assertThatThrownBy(() -> rideService.completeRide(5L))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("Ride must be ONGOING to complete");
    }

    @Test
    void cancelRide_shouldTransitionToCancelled_andReleaseDriver() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .status(RideStatus.DRIVER_ASSIGNED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

        RideResponseDto response = rideService.cancelRide(5L);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        verify(driverServiceClient).releaseDriver(101L);
    }

    @Test
    void cancelRide_shouldThrow_whenAlreadyCompleted() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .driverId(101L)
                .status(RideStatus.COMPLETED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));

        assertThatThrownBy(() -> rideService.cancelRide(5L))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("Cannot cancel a ride that is already COMPLETED");
    }

    @Test
    void cancelRide_shouldThrow_whenAlreadyCancelled() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .status(RideStatus.CANCELLED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));

        assertThatThrownBy(() -> rideService.cancelRide(5L))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("Cannot cancel a ride that is already CANCELLED");
    }

    @Test
    void cancelRide_shouldNotRelease_whenNoDriverAssigned() {
        Ride existingRide = Ride.builder()
                .id(5L)
                .riderId(1L)
                .status(RideStatus.REQUESTED)
                .build();

        when(rideRepository.findById(5L)).thenReturn(Optional.of(existingRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

        RideResponseDto response = rideService.cancelRide(5L);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        verify(driverServiceClient, never()).releaseDriver(anyLong());
    }

    @Test
    void requestRide_shouldThrow_whenRideNotFound() {
        when(rideRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getRide(999L))
                .isInstanceOf(com.ridebooking.ride.exception.RideNotFoundException.class);
    }
}
