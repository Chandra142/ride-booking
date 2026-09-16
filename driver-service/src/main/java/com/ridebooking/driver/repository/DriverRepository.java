package com.ridebooking.driver.repository;

import com.ridebooking.driver.entity.AvailabilityStatus;
import com.ridebooking.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmail(String email);

    Optional<Driver> findByPhone(String phone);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByLicenseNumber(String licenseNumber);

    @Modifying
    @Transactional
    @Query("UPDATE Driver d SET d.availabilityStatus = :targetStatus " +
            "WHERE d.id = :driverId AND d.availabilityStatus = :expectedStatus")
    int updateAvailabilityStatus(@Param("driverId") Long driverId,
                                 @Param("expectedStatus") AvailabilityStatus expectedStatus,
                                 @Param("targetStatus") AvailabilityStatus targetStatus);
}