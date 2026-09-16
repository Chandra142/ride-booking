package com.ridebooking.payment.service.impl;

import com.ridebooking.dto.PaymentRequestDTO;
import com.ridebooking.dto.PaymentResponseDTO;
import com.ridebooking.enums.PaymentStatus;
import com.ridebooking.payment.dto.gateway.PaymentGatewayResponse;
import com.ridebooking.payment.entity.Payment;
import com.ridebooking.payment.exception.ForbiddenException;
import com.ridebooking.payment.exception.InvalidPaymentStateException;
import com.ridebooking.payment.exception.ResourceNotFoundException;
import com.ridebooking.payment.gateway.PaymentGatewayService;
import com.ridebooking.payment.mapper.PaymentMapper;
import com.ridebooking.payment.repository.PaymentRepository;
import com.ridebooking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGatewayService paymentGatewayService;

    @Override
    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO requestDTO, String userId) {
        Long authenticatedUserId = parseUserId(userId);
        if (!authenticatedUserId.equals(requestDTO.getUserId())) {
            throw new ForbiddenException("You can only create payments for yourself");
        }

        String idempotencyKey = requestDTO.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> {
                        log.info("Returning existing payment for idempotency key: {}", idempotencyKey);
                        return paymentMapper.toResponseDTO(existing);
                    })
                    .orElseGet(() -> processPayment(requestDTO, idempotencyKey));
        }

        return processPayment(requestDTO, UUID.randomUUID().toString());
    }

    private PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO, String idempotencyKey) {
        Payment payment = paymentMapper.toEntity(requestDTO);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTransactionId(null);
        payment.setIdempotencyKey(idempotencyKey);

        payment = paymentRepository.save(payment);

        PaymentGatewayResponse gatewayResponse =
                paymentGatewayService.processPayment(requestDTO);

        if (gatewayResponse.isSuccess()) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayResponse.getTransactionId());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setTransactionId(null);
        }

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDTO(savedPayment);
    }

    @Override
    public List<PaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponseDTO getPaymentById(Long paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));
        verifyOwnership(payment, userId);
        return paymentMapper.toResponseDTO(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO refundPayment(Long paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        verifyOwnership(payment, userId);

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException(
                    "Cannot refund payment in " + payment.getPaymentStatus() + " state. Only SUCCESS payments can be refunded.");
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        Payment updatedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDTO(updatedPayment);
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByUser(Long userId, String authenticatedUserId) {
        Long authId = parseUserId(authenticatedUserId);
        if (!authId.equals(userId)) {
            throw new ForbiddenException("You can only view your own payments");
        }
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByRide(Long rideId) {
        return paymentRepository.findByRideId(rideId)
                .stream()
                .map(paymentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    private void verifyOwnership(Payment payment, String userId) {
        Long authId = parseUserId(userId);
        if (!authId.equals(payment.getUserId())) {
            throw new ForbiddenException("You do not have access to this payment");
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid user identity");
        }
    }
}
