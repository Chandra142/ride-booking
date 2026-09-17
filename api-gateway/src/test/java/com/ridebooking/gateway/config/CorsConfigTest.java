package com.ridebooking.gateway.config;

import org.junit.jupiter.api.Test;

class CorsConfigTest {

    @Test
    void corsConfig_canBeInstantiated() {
        CorsConfig corsConfig = new CorsConfig();
        assert corsConfig != null;
    }
}
