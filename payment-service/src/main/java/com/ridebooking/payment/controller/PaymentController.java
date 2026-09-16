package com.ridebooking.payment.controller;

import com.ridebooking.payment.common.ApiResponse;
import com.ridebooking.dto.PaymentRequestDTO;
import com.ridebooking.dto.PaymentResponseDTO;
import com.ridebooking.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> processPayment(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PaymentRequestDTO requestDTO) {

        PaymentResponseDTO response =
                paymentService.createPayment(requestDTO, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> refundPayment(
            @PathVariable Long paymentId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment refunded successfully",
                        paymentService.refundPayment(paymentId, userId)));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentById(
            @PathVariable Long paymentId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment fetched successfully",
                        paymentService.getPaymentById(paymentId, userId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentResponseDTO>>> getPaymentsByUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") String authenticatedUserId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User payments fetched successfully",
                        paymentService.getPaymentsByUser(userId, authenticatedUserId)));
    }

    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<List<PaymentResponseDTO>>> getPaymentsByRide(
            @PathVariable Long rideId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ride payments fetched successfully",
                        paymentService.getPaymentsByRide(rideId)));
    }
}
