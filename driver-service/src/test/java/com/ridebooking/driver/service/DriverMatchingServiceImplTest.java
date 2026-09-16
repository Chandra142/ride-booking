package com.ridebooking.driver.service;

import com.ridebooking.driver.config.DriverLocationProperties;
import com.ridebooking.driver.dto.DriverMatchResponse;
import com.ridebooking.driver.dto.NearbyDriverResponse;
import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverMatchingServiceImplTest {

    @Mock
    private DriverLocationService locationService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverLocationProperties properties;

    @Mock
    private StringRedisTemplate redis;

    @InjectMocks
    private DriverMatchingServiceImpl matchingService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getDefaultRadiusKm()).thenReturn(5.0);
        lenient().when(properties.getMaxRadiusKm()).thenReturn(50.0);
        lenient().when(properties.getLastSeenKeyPrefix()).thenReturn("driver:location:last-seen:");
    }

    @Test
    void shouldReturnNearestEligibleDriver() {
        NearbyDriverResponse nearDriver = NearbyDriverResponse.builder()
                .driverId(101L).distanceKm(0.5).latitude(12.976).longitude(77.605).build();
        NearbyDriverResponse farDriver = NearbyDriverResponse.builder()
                .driverId(202L).distanceKm(3.0).latitude(13.0).longitude(77.7).build();

        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(List.of(nearDriver, farDriver));
        when(redis.hasKey("driver:location:last-seen:101")).thenReturn(true);
        when(driverRepository.updateAvailabilityStatus(101L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(1);

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNotNull();
        assertThat(result.getDriverId()).isEqualTo(101L);
        assertThat(result.getDistanceKm()).isEqualTo(0.5);
    }

    @Test
    void shouldFallBackToNextDriver_whenNearestIsStale() {
        NearbyDriverResponse nearDriver = NearbyDriverResponse.builder()
                .driverId(101L).distanceKm(0.5).latitude(12.976).longitude(77.605).build();
        NearbyDriverResponse farDriver = NearbyDriverResponse.builder()
                .driverId(202L).distanceKm(3.0).latitude(13.0).longitude(77.7).build();

        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(List.of(nearDriver, farDriver));
        when(redis.hasKey("driver:location:last-seen:101")).thenReturn(false);
        when(redis.hasKey("driver:location:last-seen:202")).thenReturn(true);
        when(driverRepository.updateAvailabilityStatus(202L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(1);

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNotNull();
        assertThat(result.getDriverId()).isEqualTo(202L);
    }

    @Test
    void shouldFallBackToNextDriver_whenNearestIsNotOnline() {
        NearbyDriverResponse nearDriver = NearbyDriverResponse.builder()
                .driverId(101L).distanceKm(0.5).latitude(12.976).longitude(77.605).build();
        NearbyDriverResponse farDriver = NearbyDriverResponse.builder()
                .driverId(202L).distanceKm(3.0).latitude(13.0).longitude(77.7).build();

        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(List.of(nearDriver, farDriver));
        when(redis.hasKey("driver:location:last-seen:101")).thenReturn(true);
        when(driverRepository.updateAvailabilityStatus(101L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(0);
        when(redis.hasKey("driver:location:last-seen:202")).thenReturn(true);
        when(driverRepository.updateAvailabilityStatus(202L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(1);

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNotNull();
        assertThat(result.getDriverId()).isEqualTo(202L);
    }

    @Test
    void shouldReturnNull_whenNoNearbyDrivers() {
        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(Collections.emptyList());

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_whenAllDriversStale() {
        NearbyDriverResponse driver = NearbyDriverResponse.builder()
                .driverId(101L).distanceKm(0.5).latitude(12.976).longitude(77.605).build();

        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(List.of(driver));
        when(redis.hasKey("driver:location:last-seen:101")).thenReturn(false);

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_whenAllCandidatesOffline() {
        NearbyDriverResponse driver = NearbyDriverResponse.builder()
                .driverId(101L).distanceKm(0.5).latitude(12.976).longitude(77.605).build();

        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(List.of(driver));
        when(redis.hasKey("driver:location:last-seen:101")).thenReturn(true);
        when(driverRepository.updateAvailabilityStatus(101L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(0);

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNull();
    }

    @Test
    void shouldUseDefaultRadius_whenNegativeRadiusProvided() {
        when(locationService.findNearbyDrivers(12.9758, 77.6045, 5.0))
                .thenReturn(Collections.emptyList());

        matchingService.findAndAssignNearestDriver(12.9758, 77.6045, -1.0);

        verify(locationService).findNearbyDrivers(12.9758, 77.6045, 5.0);
    }

    @Test
    void shouldCapRadius_whenExceedsMaximum() {
        when(locationService.findNearbyDrivers(12.9758, 77.6045, 50.0))
                .thenReturn(Collections.emptyList());

        matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 100.0);

        verify(locationService).findNearbyDrivers(12.9758, 77.6045, 50.0);
    }

    @Test
    void shouldSimulateConcurrentAssignment_onlyOneWins() {
        NearbyDriverResponse driver1 = NearbyDriverResponse.builder()
                .driverId(101L).distanceKm(0.5).latitude(12.976).longitude(77.605).build();
        NearbyDriverResponse driver2 = NearbyDriverResponse.builder()
                .driverId(202L).distanceKm(1.0).latitude(12.977).longitude(77.606).build();

        when(locationService.findNearbyDrivers(12.9758, 77.6045, 10.0))
                .thenReturn(List.of(driver1, driver2));
        when(redis.hasKey("driver:location:last-seen:101")).thenReturn(true);
        when(redis.hasKey("driver:location:last-seen:202")).thenReturn(true);

        when(driverRepository.updateAvailabilityStatus(101L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(0);
        when(driverRepository.updateAvailabilityStatus(202L, AvailabilityStatus.ONLINE, AvailabilityStatus.BUSY))
                .thenReturn(1);

        DriverMatchResponse result = matchingService.findAndAssignNearestDriver(12.9758, 77.6045, 10.0);

        assertThat(result).isNotNull();
        assertThat(result.getDriverId()).isEqualTo(202L);
    }
}
