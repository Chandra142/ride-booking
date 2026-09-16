package com.ridebooking.driver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for {@code PUT /api/v1/drivers/{driverId}/location}.
 *
 * <ul>
 *   <li>{@code latitude}  — required, must be in [−90, 90].</li>
 *   <li>{@code longitude} — required, must be in [−180, 180].</li>
 * </ul>
 *
 * <p>Null is rejected by Bean Validation. {@code NaN}, {@code ±Infinity}
 * and out-of-range values are rejected by {@link
 * com.ridebooking.driver.validation.CoordinateValidator} (Bean Validation
 * cannot express "finite" rules and must not silently clamp).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverLocationRequest {

    @NotNull(message = "latitude must not be null")
    private Double latitude;

    @NotNull(message = "longitude must not be null")
    private Double longitude;
}
