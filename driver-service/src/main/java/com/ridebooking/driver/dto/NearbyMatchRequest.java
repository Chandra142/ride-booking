package com.ridebooking.driver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyMatchRequest {

    @NotNull(message = "latitude must not be null")
    private Double latitude;

    @NotNull(message = "longitude must not be null")
    private Double longitude;

    private Double radiusKm;
}
