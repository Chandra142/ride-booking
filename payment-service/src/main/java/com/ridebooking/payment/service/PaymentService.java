package com.ridebooking.payment.service;

import com.ridebooking.dto.PaymentRequestDTO;
import com.ridebooking.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    PaymentResponseDTO createPayment(PaymentRequestDTO paymentRequestDTO, String userId);

    List<PaymentResponseDTO> getAllPayments();

    PaymentResponseDTO getPaymentById(Long paymentId, String userId);

    PaymentResponseDTO refundPayment(Long paymentId, String userId);

    List<PaymentResponseDTO> getPaymentsByUser(Long userId, String authenticatedUserId);

    List<PaymentResponseDTO> getPaymentsByRide(Long rideId);

}
