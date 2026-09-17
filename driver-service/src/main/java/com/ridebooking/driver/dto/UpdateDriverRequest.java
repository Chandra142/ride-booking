package com.ridebooking.driver.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateDriverRequest {

    private String firstName;
    private String lastName;

    @Email
    private String email;

    private String phone;
    private String licenseNumber;

    private String vehicleNumber;
    private String vehicleType;
    private String vehicleModel;
    private String vehicleColor;
    private String registrationNumber;
}
