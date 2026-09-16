package com.ridebooking.dto;

import com.ridebooking.dto.validation.CardDetailsRequired;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CardDetailsRequired
public class PaymentRequestDTO {

    @NotNull(message = "Ride ID is required")
    private Long rideId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Payment Method is required")
    private String paymentMethod;

    @Valid
    private CardDetailsDTO cardDetails;

    private String idempotencyKey;
}
