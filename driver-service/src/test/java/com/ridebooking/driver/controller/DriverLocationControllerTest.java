package com.ridebooking.driver.controller;

import com.ridebooking.driver.dto.DriverLocationResponse;
import com.ridebooking.driver.dto.UpdateDriverLocationRequest;
import com.ridebooking.driver.exception.MissingIdentityException;
import com.ridebooking.driver.exception.OwnershipViolationException;
import com.ridebooking.driver.service.DriverLocationService;
import com.ridebooking.driver.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLocationControllerTest {

    @Mock
    private DriverService driverService;

    @Mock
    private DriverLocationService locationService;

    @InjectMocks
    private DriverLocationController controller;

    private UpdateDriverLocationRequest locationRequest;

    @BeforeEach
    void setUp() {
        locationRequest = UpdateDriverLocationRequest.builder()
                .latitude(12.9716)
                .longitude(77.5946)
                .build();
    }

    @Test
    void updateLocation_shouldUpdateWhenOwner() {
        doNothing().when(locationService).updateLocation(eq(1L), any(Double.class), any(Double.class));

        var response = controller.updateLocation(
                1L, "1", "DRIVER", locationRequest);

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDriverId()).isEqualTo(1L);
        assertThat(response.getBody().getLatitude()).isEqualTo(12.9716);
        assertThat(response.getBody().getLongitude()).isEqualTo(77.5946);
        verify(locationService).updateLocation(1L, 12.9716, 77.5946);
    }

    @Test
    void updateLocation_shouldAllowAdmin() {
        doNothing().when(locationService).updateLocation(eq(1L), any(Double.class), any(Double.class));

        var response = controller.updateLocation(
                1L, "99", "ADMIN", locationRequest);

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        verify(locationService).updateLocation(1L, 12.9716, 77.5946);
    }

    @Test
    void updateLocation_shouldRejectRider() {
        assertThatThrownBy(() -> controller.updateLocation(
                1L, "1", "USER", locationRequest))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessageContaining("Only drivers can update location");
    }

    @Test
    void updateLocation_shouldRejectMissingIdentity() {
        assertThatThrownBy(() -> controller.updateLocation(
                1L, null, "DRIVER", locationRequest))
                .isInstanceOf(MissingIdentityException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    void updateLocation_shouldRejectBlankIdentity() {
        assertThatThrownBy(() -> controller.updateLocation(
                1L, "  ", "DRIVER", locationRequest))
                .isInstanceOf(MissingIdentityException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    void updateLocation_shouldRejectDifferentDriver() {
        assertThatThrownBy(() -> controller.updateLocation(
                1L, "2", "DRIVER", locationRequest))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessageContaining("location cannot be updated by user");
    }

    @Test
    void updateLocation_shouldRejectInvalidIdentity() {
        assertThatThrownBy(() -> controller.updateLocation(
                1L, "invalid", "DRIVER", locationRequest))
                .isInstanceOf(MissingIdentityException.class)
                .hasMessageContaining("Invalid user identity");
    }
}
