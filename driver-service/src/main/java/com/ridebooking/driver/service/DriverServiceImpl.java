package com.ridebooking.driver.service;

import com.ridebooking.driver.dto.CreateDriverRequest;
import com.ridebooking.driver.dto.DriverResponse;
import com.ridebooking.driver.dto.UpdateDriverRequest;
import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.entity.Driver;
import com.ridebooking.driver.entity.Vehicle;
import com.ridebooking.driver.repository.DriverRepository;
import com.ridebooking.driver.repository.VehicleRepository;
import com.ridebooking.driver.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public DriverResponse createDriver(CreateDriverRequest request) {

        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (driverRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException("License number already exists");
        }

        if (vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new IllegalArgumentException("Vehicle registration already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber())
                .vehicleType(request.getVehicleType())
                .vehicleModel(request.getVehicleModel())
                .vehicleColor(request.getVehicleColor())
                .registrationNumber(request.getRegistrationNumber())
                .build();

        Driver driver = Driver.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .licenseNumber(request.getLicenseNumber())
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .rating(0.0)
                .vehicle(vehicle)
                .build();

        Driver savedDriver = driverRepository.save(driver);

        return mapToResponse(savedDriver);
    }

    @Override
    public DriverResponse getDriverById(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        return mapToResponse(driver);
    }

    @Override
    public List<DriverResponse> getAllDrivers() {

        return driverRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public DriverResponse updateDriver(Long id, UpdateDriverRequest request) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (request.getFirstName() != null)
            driver.setFirstName(request.getFirstName());

        if (request.getLastName() != null)
            driver.setLastName(request.getLastName());

        if (request.getEmail() != null)
            driver.setEmail(request.getEmail());

        if (request.getPhone() != null)
            driver.setPhone(request.getPhone());

        if (request.getLicenseNumber() != null)
            driver.setLicenseNumber(request.getLicenseNumber());

        if (request.getAvailabilityStatus() != null)
            driver.setAvailabilityStatus(request.getAvailabilityStatus());

        Vehicle vehicle = driver.getVehicle();

        if (request.getVehicleNumber() != null)
            vehicle.setVehicleNumber(request.getVehicleNumber());

        if (request.getVehicleType() != null)
            vehicle.setVehicleType(request.getVehicleType());

        if (request.getVehicleModel() != null)
            vehicle.setVehicleModel(request.getVehicleModel());

        if (request.getVehicleColor() != null)
            vehicle.setVehicleColor(request.getVehicleColor());

        if (request.getRegistrationNumber() != null)
            vehicle.setRegistrationNumber(request.getRegistrationNumber());

        Driver updatedDriver = driverRepository.save(driver);

        return mapToResponse(updatedDriver);
    }

    @Override
    public void deleteDriver(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));;

        driverRepository.delete(driver);
    }

    @Override
    public DriverResponse updateAvailability(Long id, AvailabilityStatus status) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));;

        driver.setAvailabilityStatus(status);

        Driver updatedDriver = driverRepository.save(driver);

        return mapToResponse(updatedDriver);
    }

    @Override
    public DriverResponse releaseDriver(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (driver.getAvailabilityStatus() == AvailabilityStatus.ONLINE) {
            return mapToResponse(driver);
        }

        driver.setAvailabilityStatus(AvailabilityStatus.ONLINE);
        Driver updatedDriver = driverRepository.save(driver);

        return mapToResponse(updatedDriver);
    }

    private DriverResponse mapToResponse(Driver driver) {

        return DriverResponse.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .availabilityStatus(driver.getAvailabilityStatus())
                .rating(driver.getRating())
                .vehicleNumber(driver.getVehicle().getVehicleNumber())
                .vehicleType(driver.getVehicle().getVehicleType())
                .vehicleModel(driver.getVehicle().getVehicleModel())
                .vehicleColor(driver.getVehicle().getVehicleColor())
                .registrationNumber(driver.getVehicle().getRegistrationNumber())
                .build();
    }
}