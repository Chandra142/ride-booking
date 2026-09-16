package com.ridebooking.driver.service;

import com.ridebooking.driver.dto.DriverMatchResponse;

public interface DriverMatchingService {

    DriverMatchResponse findAndAssignNearestDriver(double pickupLatitude, double pickupLongitude, double radiusKm);
}
