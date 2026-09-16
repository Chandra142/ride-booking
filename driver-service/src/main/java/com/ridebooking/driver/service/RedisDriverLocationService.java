package com.ridebooking.driver.service;

import com.ridebooking.driver.config.DriverLocationProperties;
import com.ridebooking.driver.dto.NearbyDriverResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisDriverLocationService implements DriverLocationService {

    private final StringRedisTemplate redis;
    private final DriverLocationProperties properties;

    @Override
    public void updateLocation(Long driverId, double latitude, double longitude) {
        String geoKey = properties.getGeoKey();
        String member = properties.getMemberKeyPrefix() + driverId;
        redis.opsForGeo().add(geoKey, new Point(longitude, latitude), member);

        String lastSeenKey = properties.getLastSeenKeyPrefix() + driverId;
        Duration ttl = Duration.ofSeconds(properties.getTtlSeconds());
        redis.opsForValue().set(lastSeenKey, Instant.now().toString(), ttl);
    }

    @Override
    public void removeLocation(Long driverId) {
        String geoKey = properties.getGeoKey();
        String member = properties.getMemberKeyPrefix() + driverId;
        redis.opsForGeo().remove(geoKey, member);

        String lastSeenKey = properties.getLastSeenKeyPrefix() + driverId;
        redis.delete(lastSeenKey);
    }

    @Override
    public List<NearbyDriverResponse> findNearbyDrivers(double latitude, double longitude, double radiusKm) {
        String geoKey = properties.getGeoKey();
        Circle circle = new Circle(new Point(longitude, latitude),
                new Distance(radiusKm, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redis.opsForGeo().radius(geoKey, circle,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance().sortAscending());

        List<NearbyDriverResponse> nearby = new ArrayList<>();
        if (results == null) {
            return nearby;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            RedisGeoCommands.GeoLocation<String> loc = result.getContent();
            String member = loc.getName();
            String driverId = member.substring(properties.getMemberKeyPrefix().length());
            Point p = loc.getPoint();
            double distanceKm = result.getDistance().getValue();
            nearby.add(NearbyDriverResponse.builder()
                    .driverId(Long.valueOf(driverId))
                    .latitude(p.getY())
                    .longitude(p.getX())
                    .distanceKm(distanceKm)
                    .build());
        }
        return nearby;
    }
}
