package com.ridebooking.driver.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis connectivity for the driver location index (Stage 3).
 *
 * <p>Uses Lettuce (thread-safe, non-blocking) and builds the standalone
 * connection from {@code spring.data.redis.*} (host/port/username/password)
 * so the same configuration block works for a local container and a managed
 * Redis in prod. No single GEO-key TTL is used — see
 * {@link DriverLocationProperties} for the key-freshness design.</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                properties.getHost(), properties.getPort());
        if (properties.getUsername() != null) {
            config.setUsername(properties.getUsername());
        }
        if (properties.getPassword() != null) {
            config.setPassword(properties.getPassword());
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
