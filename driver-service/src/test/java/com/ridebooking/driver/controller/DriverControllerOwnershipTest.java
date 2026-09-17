package com.ridebooking.driver.controller;

import com.ridebooking.driver.dto.UpdateDriverRequest;
import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.exception.ForbiddenException;
import com.ridebooking.driver.exception.MissingIdentityException;
import com.ridebooking.driver.exception.OwnershipViolationException;
import com.ridebooking.driver.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverControllerOwnershipTest {

    @Mock
    private DriverService driverService;

    @InjectMocks
    private DriverController controller;

    @Test
    void updateDriver_shouldRejectMissingIdentity() {
        assertThatThrownBy(() -> controller.updateDriver(
                1L, null, "DRIVER", new UpdateDriverRequest()))
                .isInstanceOf(MissingIdentityException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    void updateDriver_shouldRejectNonDriver() {
        assertThatThrownBy(() -> controller.updateDriver(
                1L, "1", "USER", new UpdateDriverRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only drivers can perform this action");
    }

    @Test
    void updateDriver_shouldRejectDifferentDriver() {
        assertThatThrownBy(() -> controller.updateDriver(
                1L, "2", "DRIVER", new UpdateDriverRequest()))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessageContaining("is not owned by user");
    }

    @Test
    void updateDriver_shouldAllowOwner() {
        when(driverService.updateDriver(eq(1L), any(UpdateDriverRequest.class))).thenReturn(null);

        controller.updateDriver(1L, "1", "DRIVER", new UpdateDriverRequest());

        verify(driverService).updateDriver(eq(1L), any(UpdateDriverRequest.class));
    }

    @Test
    void updateDriver_shouldAllowAdmin() {
        when(driverService.updateDriver(eq(1L), any(UpdateDriverRequest.class))).thenReturn(null);

        controller.updateDriver(1L, "99", "ADMIN", new UpdateDriverRequest());

        verify(driverService).updateDriver(eq(1L), any(UpdateDriverRequest.class));
    }

    @Test
    void deleteDriver_shouldRejectMissingIdentity() {
        assertThatThrownBy(() -> controller.deleteDriver(1L, null, "DRIVER"))
                .isInstanceOf(MissingIdentityException.class);
    }

    @Test
    void deleteDriver_shouldRejectDifferentDriver() {
        assertThatThrownBy(() -> controller.deleteDriver(1L, "2", "DRIVER"))
                .isInstanceOf(OwnershipViolationException.class);
    }

    @Test
    void deleteDriver_shouldAllowOwner() {
        doNothing().when(driverService).deleteDriver(1L);

        controller.deleteDriver(1L, "1", "DRIVER");

        verify(driverService).deleteDriver(1L);
    }

    @Test
    void updateAvailability_shouldRejectDifferentDriver() {
        assertThatThrownBy(() -> controller.updateAvailability(
                1L, "2", "DRIVER", AvailabilityStatus.ONLINE))
                .isInstanceOf(OwnershipViolationException.class);
    }

    @Test
    void updateAvailability_shouldAllowOwner() {
        when(driverService.updateAvailability(eq(1L), any())).thenReturn(null);

        controller.updateAvailability(1L, "1", "DRIVER", AvailabilityStatus.ONLINE);

        verify(driverService).updateAvailability(1L, AvailabilityStatus.ONLINE);
    }

    @Test
    void releaseDriver_shouldRejectDifferentDriver() {
        assertThatThrownBy(() -> controller.releaseDriver(1L, "2", "DRIVER"))
                .isInstanceOf(OwnershipViolationException.class);
    }

    @Test
    void releaseDriver_shouldAllowOwner() {
        when(driverService.releaseDriver(1L)).thenReturn(null);

        controller.releaseDriver(1L, "1", "DRIVER");

        verify(driverService).releaseDriver(1L);
    }

    @Test
    void enforceOwnership_shouldRejectInvalidUserId() {
        assertThatThrownBy(() -> controller.updateDriver(
                1L, "invalid", "DRIVER", new UpdateDriverRequest()))
                .isInstanceOf(MissingIdentityException.class)
                .hasMessageContaining("Invalid user identity");
    }
}
