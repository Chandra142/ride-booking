package com.ridebooking.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverMatchResult {
    private Long driverId;
    private Double distanceKm;
}
