package com.ridebooking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (!isValidCorrelationId(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.substring(0, Math.min(correlationId.length(), MAX_CORRELATION_ID_LENGTH));
        }

        final String finalCorrelationId = correlationId;

        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        log.debug("Correlation ID: {}", finalCorrelationId);

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .contextWrite(Context.of(CORRELATION_ID_HEADER, finalCorrelationId));
    }

    private boolean isValidCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            return false;
        }
        if (correlationId.length() > MAX_CORRELATION_ID_LENGTH) {
            return false;
        }
        return correlationId.matches("[a-zA-Z0-9\\-_]+");
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
