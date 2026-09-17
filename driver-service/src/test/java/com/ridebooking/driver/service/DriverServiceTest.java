package com.ridebooking.driver.service;

import com.ridebooking.driver.dto.CreateDriverRequest;
import com.ridebooking.driver.dto.DriverResponse;
import com.ridebooking.driver.dto.UpdateDriverRequest;
import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.entity.Driver;
import com.ridebooking.driver.entity.Vehicle;
import com.ridebooking.driver.exception.InvalidStateException;
import com.ridebooking.driver.exception.ResourceNotFoundException;
import com.ridebooking.driver.repository.DriverRepository;
import com.ridebooking.driver.repository.VehicleRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverServiceImpl driverService;

    private Driver testDriver;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        testVehicle = Vehicle.builder()
                .id(1L)
                .vehicleNumber("ABC-1234")
                .vehicleType("Sedan")
                .vehicleModel("Toyota Camry")
                .vehicleColor("White")
                .registrationNumber("KA-01-AB-1234")
                .build();

        testDriver = Driver.builder()
                .id(1L)
                .firstName("John")
                .lastName("Driver")
                .email("john@driver.com")
                .phone("1234567890")
                .licenseNumber("DL-123456")
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .rating(4.5)
                .vehicle(testVehicle)
                .build();
    }

    @Test
    void createDriver_shouldReturnDriverResponse() {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setFirstName("John");
        request.setLastName("Driver");
        request.setEmail("john@driver.com");
        request.setPhone("1234567890");
        request.setLicenseNumber("DL-123456");
        request.setVehicleNumber("ABC-1234");
        request.setVehicleType("Sedan");
        request.setVehicleModel("Toyota Camry");
        request.setVehicleColor("White");
        request.setRegistrationNumber("KA-01-AB-1234");

        when(driverRepository.existsByEmail("john@driver.com")).thenReturn(false);
        when(driverRepository.existsByPhone("1234567890")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-123456")).thenReturn(false);
        when(vehicleRepository.existsByRegistrationNumber("KA-01-AB-1234")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(testDriver);

        DriverResponse response = driverService.createDriver(request);

        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Driver");
        assertThat(response.getEmail()).isEqualTo("john@driver.com");
        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }

    @Test
    void createDriver_shouldThrowWhenEmailExists() {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setEmail("john@driver.com");

        when(driverRepository.existsByEmail("john@driver.com")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void createDriver_shouldThrowWhenPhoneExists() {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setEmail("new@driver.com");
        request.setPhone("1234567890");

        when(driverRepository.existsByEmail("new@driver.com")).thenReturn(false);
        when(driverRepository.existsByPhone("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number already exists");
    }

    @Test
    void createDriver_shouldThrowWhenLicenseExists() {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setEmail("new@driver.com");
        request.setPhone("9999999999");
        request.setLicenseNumber("DL-123456");

        when(driverRepository.existsByEmail("new@driver.com")).thenReturn(false);
        when(driverRepository.existsByPhone("9999999999")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-123456")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("License number already exists");
    }

    @Test
    void getDriverById_shouldReturnDriver() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        DriverResponse response = driverService.getDriverById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
    }

    @Test
    void getDriverById_shouldThrowWhenNotFound() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getDriverById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    void getAllDrivers_shouldReturnList() {
        when(driverRepository.findAll()).thenReturn(List.of(testDriver));

        List<DriverResponse> drivers = driverService.getAllDrivers();

        assertThat(drivers).hasSize(1);
        assertThat(drivers.get(0).getEmail()).isEqualTo("john@driver.com");
    }

    @Test
    void updateDriver_shouldUpdateFields() {
        UpdateDriverRequest request = new UpdateDriverRequest();
        request.setFirstName("Jane");
        request.setPhone("9999999999");

        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateDriver(1L, request);

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getPhone()).isEqualTo("9999999999");
        assertThat(response.getLastName()).isEqualTo("Driver");
    }

    @Test
    void updateDriver_shouldThrowWhenNotFound() {
        UpdateDriverRequest request = new UpdateDriverRequest();
        request.setFirstName("Test");

        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.updateDriver(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    void deleteDriver_shouldDelete() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));
        doNothing().when(driverRepository).delete(testDriver);

        driverService.deleteDriver(1L);

        verify(driverRepository).delete(testDriver);
    }

    @Test
    void deleteDriver_shouldThrowWhenNotFound() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.deleteDriver(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    void updateAvailability_shouldTransitionOfflineToOnline() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateAvailability(1L, AvailabilityStatus.ONLINE);

        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.ONLINE);
    }

    @Test
    void updateAvailability_shouldTransitionOnlineToOffline() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.ONLINE);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateAvailability(1L, AvailabilityStatus.OFFLINE);

        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }

    @Test
    void updateAvailability_shouldAllowSameStatus() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateAvailability(1L, AvailabilityStatus.OFFLINE);

        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }

    @Test
    void updateAvailability_shouldRejectBusyToOnline() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.BUSY);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        assertThatThrownBy(() -> driverService.updateAvailability(1L, AvailabilityStatus.ONLINE))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot transition from BUSY to ONLINE");
    }

    @Test
    void updateAvailability_shouldRejectBusyToOffline() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.BUSY);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        assertThatThrownBy(() -> driverService.updateAvailability(1L, AvailabilityStatus.OFFLINE))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot transition from BUSY to OFFLINE");
    }

    @Test
    void updateAvailability_shouldRejectOnlineToBusy() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.ONLINE);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        assertThatThrownBy(() -> driverService.updateAvailability(1L, AvailabilityStatus.BUSY))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot transition from ONLINE to BUSY");
    }

    @Test
    void updateAvailability_shouldRejectOfflineToBusy() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        assertThatThrownBy(() -> driverService.updateAvailability(1L, AvailabilityStatus.BUSY))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot transition from OFFLINE to BUSY");
    }

    @Test
    void updateAvailability_shouldThrowWhenDriverNotFound() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.updateAvailability(99L, AvailabilityStatus.ONLINE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    void releaseDriver_shouldTransitionBusyToOnline() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.BUSY);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.releaseDriver(1L);

        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.ONLINE);
    }

    @Test
    void releaseDriver_shouldNotChangeIfAlreadyOnline() {
        testDriver.setAvailabilityStatus(AvailabilityStatus.ONLINE);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        DriverResponse response = driverService.releaseDriver(1L);

        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.ONLINE);
        verify(driverRepository, never()).save(any());
    }

    @Test
    void releaseDriver_shouldThrowWhenDriverNotFound() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.releaseDriver(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    void mapToResponse_shouldFlattenVehicle() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(testDriver));

        DriverResponse response = driverService.getDriverById(1L);

        assertThat(response.getVehicleNumber()).isEqualTo("ABC-1234");
        assertThat(response.getVehicleType()).isEqualTo("Sedan");
        assertThat(response.getVehicleModel()).isEqualTo("Toyota Camry");
        assertThat(response.getVehicleColor()).isEqualTo("White");
        assertThat(response.getRegistrationNumber()).isEqualTo("KA-01-AB-1234");
    }
}
