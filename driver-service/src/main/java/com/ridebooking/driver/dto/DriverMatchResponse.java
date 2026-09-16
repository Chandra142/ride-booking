package com.ridebooking.driver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverMatchResponse {
    private Long driverId;
    private Double distanceKm;
}
