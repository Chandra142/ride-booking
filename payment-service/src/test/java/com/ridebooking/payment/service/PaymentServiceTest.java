package com.ridebooking.payment.service;

import com.ridebooking.dto.CardDetailsDTO;
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
import com.ridebooking.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @Mock
    private PaymentGatewayService paymentGatewayService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequestDTO paymentRequestDTO;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequestDTO = PaymentRequestDTO.builder()
                .rideId(100L)
                .userId(10L)
                .amount(BigDecimal.valueOf(150.0))
                .paymentMethod("CARD")
                .cardDetails(CardDetailsDTO.builder()
                        .cardNumber("1234567890123456")
                        .cardHolderName("John Doe")
                        .expiryDate("12/28")
                        .cvv("123")
                        .build())
                .build();

        payment = Payment.builder()
                .paymentId(1L)
                .rideId(100L)
                .userId(10L)
                .amount(BigDecimal.valueOf(150.0))
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.SUCCESS)
                .transactionId("CARD-TXN-123")
                .paymentTime(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreatePaymentSuccess() {
        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .success(true)
                .transactionId("CARD-TXN-123")
                .gatewayMessage("Payment processed successfully")
                .build();

        when(paymentGatewayService.processPayment(any(PaymentRequestDTO.class))).thenReturn(gatewayResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(1L);
            }
            return p;
        });

        PaymentResponseDTO response = paymentService.createPayment(paymentRequestDTO, "10");

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals("CARD-TXN-123", response.getTransactionId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void testCreatePaymentFailure() {
        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .success(false)
                .transactionId(null)
                .gatewayMessage("Limit exceeded")
                .build();

        when(paymentGatewayService.processPayment(any(PaymentRequestDTO.class))).thenReturn(gatewayResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(1L);
            }
            return p;
        });

        PaymentResponseDTO response = paymentService.createPayment(paymentRequestDTO, "10");

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        assertNull(response.getTransactionId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void testCreatePayment_shouldThrow_whenNotOwnPayment() {
        paymentRequestDTO.setUserId(20L);

        assertThrows(ForbiddenException.class,
                () -> paymentService.createPayment(paymentRequestDTO, "10"));
    }

    @Test
    void testCreatePayment_idempotent() {
        PaymentRequestDTO requestWithKey = PaymentRequestDTO.builder()
                .rideId(100L).userId(10L)
                .amount(BigDecimal.valueOf(150.0))
                .paymentMethod("CARD")
                .idempotencyKey("idem-123")
                .build();

        when(paymentRepository.findByIdempotencyKey("idem-123")).thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.createPayment(requestWithKey, "10");

        assertNotNull(response);
        assertEquals(1L, response.getPaymentId());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testGetAllPayments() {
        when(paymentRepository.findAll()).thenReturn(Collections.singletonList(payment));

        List<PaymentResponseDTO> response = paymentService.getAllPayments();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getPaymentId());
    }

    @Test
    void testGetPaymentByIdSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentById(1L, "10");

        assertNotNull(response);
        assertEquals(1L, response.getPaymentId());
    }

    @Test
    void testGetPaymentById_shouldThrow_whenNotOwner() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(ForbiddenException.class,
                () -> paymentService.getPaymentById(1L, "999"));
    }

    @Test
    void testGetPaymentByIdNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(1L, "10"));
    }

    @Test
    void testRefundPaymentSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDTO response = paymentService.refundPayment(1L, "10");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getPaymentStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void testRefundPayment_shouldThrow_whenNotOwner() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(ForbiddenException.class,
                () -> paymentService.refundPayment(1L, "999"));
    }

    @Test
    void testRefundPayment_shouldThrow_whenNotSuccessful() {
        Payment failedPayment = Payment.builder()
                .paymentId(2L).rideId(100L).userId(10L)
                .paymentStatus(PaymentStatus.FAILED).build();
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(failedPayment));

        assertThrows(InvalidPaymentStateException.class,
                () -> paymentService.refundPayment(2L, "10"));
    }

    @Test
    void testGetPaymentsByUser() {
        when(paymentRepository.findByUserId(10L)).thenReturn(Collections.singletonList(payment));

        List<PaymentResponseDTO> response = paymentService.getPaymentsByUser(10L, "10");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).getUserId());
    }

    @Test
    void testGetPaymentsByUser_shouldThrow_whenNotOwner() {
        assertThrows(ForbiddenException.class,
                () -> paymentService.getPaymentsByUser(10L, "999"));
    }

    @Test
    void testGetPaymentsByRide() {
        when(paymentRepository.findByRideId(100L)).thenReturn(Collections.singletonList(payment));

        List<PaymentResponseDTO> response = paymentService.getPaymentsByRide(100L);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(100L, response.get(0).getRideId());
    }
}
