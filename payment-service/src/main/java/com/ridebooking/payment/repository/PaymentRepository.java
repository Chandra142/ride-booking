package com.ridebooking.payment.repository;

import com.ridebooking.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByRideId(Long rideId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

}
