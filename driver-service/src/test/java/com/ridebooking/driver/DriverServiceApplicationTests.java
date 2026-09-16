package com.ridebooking.driver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.ridebooking.driver.config.DriverLocationProperties;

@SpringBootTest
class DriverServiceApplicationTests {

    @Autowired
    private DriverLocationProperties properties;

    @Test
    void contextLoads() {
    }

    @Test
    void driverLocationPropertiesIsAvailableInContext() {
        org.junit.jupiter.api.Assertions.assertNotNull(properties);
        org.junit.jupiter.api.Assertions.assertEquals("drivers:locations", properties.getGeoKey());
    }
}
