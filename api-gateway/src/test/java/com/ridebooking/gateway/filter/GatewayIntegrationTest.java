package com.ridebooking.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GatewayIntegrationTest {

    private CorrelationIdFilter correlationIdFilter;
    private SecurityHeadersFilter securityHeadersFilter;
    private JwtAuthenticationFilter jwtFilter;

    private static final String JWT_SECRET = "test-secret-key-for-unit-tests-only-32-bytes!";

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
        securityHeadersFilter = new SecurityHeadersFilter();
        jwtFilter = new JwtAuthenticationFilter();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void correlationId_shouldGenerateWhenMissing() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        correlationIdFilter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse().getHeaders().getFirst("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
    }

    @Test
    void correlationId_shouldPreserveWhenProvided() throws Exception {
        String testId = "test-correlation-123";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .header("X-Correlation-ID", testId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        correlationIdFilter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse().getHeaders().getFirst("X-Correlation-ID");
        assertEquals(testId, correlationId);
    }

    @Test
    void correlationId_shouldRejectInvalidFormat() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .header("X-Correlation-ID", "id with spaces!")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        correlationIdFilter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse().getHeaders().getFirst("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.contains(" "));
    }

    @Test
    void correlationId_shouldTruncateLongId() throws Exception {
        String longId = "a".repeat(200);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .header("X-Correlation-ID", longId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        correlationIdFilter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse().getHeaders().getFirst("X-Correlation-ID");
        assertNotNull(correlationId);
        assertTrue(correlationId.length() <= 128);
    }

    @Test
    void securityHeaders_shouldBeAdded() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        securityHeadersFilter.filter(exchange, chain).block();

        assertEquals("nosniff", exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin", exchange.getResponse().getHeaders().getFirst("Referrer-Policy"));
        assertEquals("1; mode=block", exchange.getResponse().getHeaders().getFirst("X-XSS-Protection"));
        assertEquals("no-store, no-cache, must-revalidate", exchange.getResponse().getHeaders().getFirst("Cache-Control"));
        assertEquals("no-cache", exchange.getResponse().getHeaders().getFirst("Pragma"));
    }

    @Test
    void jwtFilter_shouldAllowPublicEndpoint() throws Exception {
        setField(jwtFilter, "jwtSecret", JWT_SECRET);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        jwtFilter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void jwtFilter_shouldRejectWithoutToken() throws Exception {
        setField(jwtFilter, "jwtSecret", JWT_SECRET);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/users/1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        jwtFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void jwtFilter_shouldRejectInvalidToken() throws Exception {
        setField(jwtFilter, "jwtSecret", JWT_SECRET);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        jwtFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void jwtFilter_shouldAcceptValidToken() throws Exception {
        setField(jwtFilter, "jwtSecret", JWT_SECRET);

        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .claim("email", "test@test.com")
                .claim("role", "RIDER")
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        jwtFilter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void jwtFilter_shouldStripSpoofedHeaders() throws Exception {
        setField(jwtFilter, "jwtSecret", JWT_SECRET);

        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("real-user-id")
                .claim("email", "real@test.com")
                .claim("role", "RIDER")
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "spoofed-id")
                .header("X-User-Email", "spoofed@email.com")
                .header("X-User-Role", "ROLE_ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        jwtFilter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());

        ServerWebExchange capturedExchange = captor.getValue();
        assertEquals("real-user-id", capturedExchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("real@test.com", capturedExchange.getRequest().getHeaders().getFirst("X-User-Email"));
        assertEquals("RIDER", capturedExchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void jwtFilter_shouldReturnEmptyRoleWhenMissing() throws Exception {
        setField(jwtFilter, "jwtSecret", JWT_SECRET);

        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .claim("email", "test@test.com")
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        jwtFilter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());

        ServerWebExchange capturedExchange = captor.getValue();
        assertEquals("", capturedExchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }
}
