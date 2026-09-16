package com.ridebooking.driver.service;

import com.ridebooking.driver.dto.CreateDriverRequest;
import com.ridebooking.driver.dto.DriverResponse;
import com.ridebooking.driver.dto.UpdateDriverRequest;
import com.ridebooking.driver.entity.AvailabilityStatus;

import java.util.List;

public interface DriverService {

    DriverResponse createDriver(CreateDriverRequest request);

    DriverResponse getDriverById(Long id);

    List<DriverResponse> getAllDrivers();

    DriverResponse updateDriver(Long id, UpdateDriverRequest request);

    void deleteDriver(Long id);

    DriverResponse updateAvailability(Long id, AvailabilityStatus status);

    DriverResponse releaseDriver(Long id);
}